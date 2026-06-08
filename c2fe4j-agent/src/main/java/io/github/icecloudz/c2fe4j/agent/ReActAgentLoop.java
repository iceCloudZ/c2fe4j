package io.github.icecloudz.c2fe4j.agent;

import io.github.icecloudz.c2fe4j.core.*;
import io.github.icecloudz.c2fe4j.core.tool.*;
import io.github.icecloudz.c2fe4j.obs.Observability;
import io.github.icecloudz.c2fe4j.obs.Span;
import io.github.icecloudz.c2fe4j.obs.TokenUsage;
import io.github.icecloudz.c2fe4j.obs.Trace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ReAct (Reason-Act) agent loop implementation.
 *
 * Cycle: LLM thinks → decides to use tools or finish → execute tools → feed results back.
 * Respects P3: LLM decides when to stop, not hard-coded step limits.
 * Respects P5: transparent about tool execution results.
 */
public class ReActAgentLoop implements AgentLoop {

    private static final Logger log = LoggerFactory.getLogger(ReActAgentLoop.class);

    private final LlmClient llmClient;
    private final Map<String, ToolExecutor> toolExecutors;
    private final List<ToolDefinition> toolDefinitions;
    private final DecisionGate decisionGate;
    private final Observability observability;
    private final int maxSteps;

    public ReActAgentLoop(LlmClient llmClient,
                          List<ToolDefinition> tools,
                          Map<String, ToolExecutor> executors,
                          DecisionGate decisionGate,
                          Observability observability,
                          int maxSteps) {
        this.llmClient = llmClient;
        this.toolDefinitions = tools;
        this.toolExecutors = new ConcurrentHashMap<>(executors);
        this.decisionGate = decisionGate;
        this.observability = observability;
        this.maxSteps = maxSteps;
    }

    @Override
    public AgentResult run(AgentContext context) {
        Trace trace = observability.startTrace(context.sessionId());
        long startTime = System.currentTimeMillis();

        try {
            while (context.stepCount() < maxSteps) {
                StepResult stepResult = step(context);
                trace.addSpan(observability.startSpan(trace.traceId(), "step", "step-" + context.stepCount()));

                if (stepResult.finished()) {
                    trace.complete();
                    long latency = System.currentTimeMillis() - startTime;
                    return new AgentResult(
                            stepResult.response().content(),
                            context,
                            stepResult.response().tokenUsage(),
                            context.stepCount(),
                            latency
                    );
                }
            }

            // Reached max steps — transparent about it (P5)
            String msg = "Agent loop reached %d steps. Providing best answer so far.".formatted(maxSteps);
            log.info(msg);
            trace.complete();
            long latency = System.currentTimeMillis() - startTime;
            return new AgentResult(
                    context.messages().getLast().content(),
                    context,
                    ChatResponse.TokenUsage.empty(),
                    context.stepCount(),
                    latency
            );
        } catch (Exception e) {
            trace.fail(e.getMessage());
            throw e;
        }
    }

    @Override
    public StepResult step(AgentContext context) {
        context.incrementStep();
        Span span = observability.startSpan(null, "llm", "chat-step-" + context.stepCount());

        ChatResponse response = llmClient.chat(context.messages(), toolDefinitions);
        context.addTokens(response.tokenUsage().totalTokens());

        // Check if LLM made tool calls
        List<ToolCall> toolCalls = OpenAiToolCallParser.parse(response.content());

        if (toolCalls.isEmpty() || "stop".equals(response.finishReason())) {
            // No tool calls — LLM decided to answer directly
            context.addMessage(ChatMessage.assistant(OpenAiToolCallParser.strip(response.content())));
            return StepResult.done(response);
        }

        // LLM wants to use tools — execute them transparently
        context.addMessage(ChatMessage.assistant(OpenAiToolCallParser.strip(response.content())));

        for (ToolCall call : toolCalls) {
            Span toolSpan = observability.startChildSpan(span, "tool", call.name());
            ToolExecutor executor = toolExecutors.get(call.name());

            ToolResult result;
            if (executor == null) {
                // P5: transparent — tell LLM the tool doesn't exist
                result = ToolResult.error(call.id(),
                        "Tool '%s' is not available. Available tools: %s".formatted(call.name(), toolExecutors.keySet()));
            } else {
                try {
                    result = executor.execute(call);
                } catch (Exception e) {
                    result = ToolResult.error(call.id(),
                            "Tool '%s' failed: %s".formatted(call.name(), e.getMessage()));
                }
            }

            // Feed tool result back as TOOL message
            context.addMessage(new ChatMessage(ChatMessage.Role.TOOL, result.content()));
            observability.endSpan(toolSpan, TokenUsage.empty());
        }

        return StepResult.ongoing(response);
    }
}
