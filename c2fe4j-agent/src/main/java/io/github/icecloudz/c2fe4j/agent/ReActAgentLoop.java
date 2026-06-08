package io.github.icecloudz.c2fe4j.agent;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import io.github.icecloudz.c2fe4j.obs.Observability;
import io.github.icecloudz.c2fe4j.obs.Span;
import io.github.icecloudz.c2fe4j.obs.Trace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;

/**
 * ReAct (Reason-Act) agent loop implementation.
 *
 * Cycle: LLM thinks -> decides to use tools or finish -> execute tools -> feed results back.
 * P3: LLM decides when to stop. P5: transparent about tool execution results.
 */
public class ReActAgentLoop {

    private static final Logger log = LoggerFactory.getLogger(ReActAgentLoop.class);

    private final ChatModel chatModel;
    private final List<ToolSpecification> toolSpecifications;
    private final Function<ToolExecutionRequest, String> toolExecutor;
    private final Observability observability;
    private final int maxSteps;

    public ReActAgentLoop(ChatModel chatModel,
                          List<ToolSpecification> tools,
                          Function<ToolExecutionRequest, String> toolExecutor,
                          Observability observability,
                          int maxSteps) {
        this.chatModel = chatModel;
        this.toolSpecifications = tools != null ? tools : List.of();
        this.toolExecutor = toolExecutor;
        this.observability = observability;
        this.maxSteps = maxSteps;
    }

    /**
     * Run the ReAct loop to completion.
     */
    public ReActResult run(List<ChatMessage> messages, String sessionId) {
        Trace trace = observability.startTrace(sessionId);
        long startTime = System.currentTimeMillis();

        List<ChatMessage> conversation = new ArrayList<>(messages);
        int steps = 0;
        TokenUsage totalUsage = null;

        try {
            while (steps < maxSteps) {
                steps++;
                Span span = observability.startSpan(trace.traceId(), "llm", "step-" + steps);

                ChatResponse response = chatModel.chat(conversation);
                AiMessage aiMessage = response.aiMessage();

                totalUsage = TokenUsage.sum(totalUsage, response.tokenUsage());

                conversation.add(aiMessage);

                // No tool calls — LLM decided to answer directly
                if (!aiMessage.hasToolExecutionRequests()) {
                    trace.complete();
                    long latency = System.currentTimeMillis() - startTime;
                    return new ReActResult(
                            aiMessage.text(),
                            conversation,
                            totalUsage,
                            steps,
                            latency
                    );
                }

                // Execute tool calls transparently (P5)
                for (ToolExecutionRequest request : aiMessage.toolExecutionRequests()) {
                    Span toolSpan = observability.startChildSpan(span, "tool", request.name());
                    String result;
                    try {
                        result = toolExecutor.apply(request);
                    } catch (Exception e) {
                        result = "Tool '%s' failed: %s".formatted(request.name(), e.getMessage());
                    }
                    conversation.add(ToolExecutionResultMessage.from(request, result));
                    observability.endSpan(toolSpan, null);
                }
            }

            // Max steps reached
            String lastText = getLastText(conversation);
            log.info("ReAct loop reached {} steps", maxSteps);
            trace.complete();
            return new ReActResult(lastText, conversation, totalUsage, steps, System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            trace.fail(e.getMessage());
            throw e;
        }
    }

    private String getLastText(List<ChatMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i) instanceof AiMessage ai && ai.text() != null) {
                return ai.text();
            }
        }
        return "";
    }

    /**
     * Result of a ReAct loop execution.
     */
    public record ReActResult(
            String content,
            List<ChatMessage> conversation,
            TokenUsage tokenUsage,
            int steps,
            long latencyMs
    ) {}
}
