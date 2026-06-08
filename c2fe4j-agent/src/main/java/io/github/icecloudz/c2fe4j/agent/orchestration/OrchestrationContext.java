package io.github.icecloudz.c2fe4j.agent.orchestration;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Context for orchestration strategy execution.
 */
public class OrchestrationContext {

    private final List<ChatMessage> messages = new ArrayList<>();
    private List<ToolSpecification> tools = List.of();
    private Function<ToolExecutionRequest, String> toolExecutor;
    private ChatModel chatModel;
    private String sessionId;
    private String model;
    private int maxSteps = 10;
    private String systemPrompt;

    public OrchestrationContext sessionId(String sessionId) { this.sessionId = sessionId; return this; }
    public OrchestrationContext model(String model) { this.model = model; return this; }
    public OrchestrationContext maxSteps(int maxSteps) { this.maxSteps = maxSteps; return this; }
    public OrchestrationContext systemPrompt(String sp) { this.systemPrompt = sp; return this; }
    public OrchestrationContext tools(List<ToolSpecification> tools) { this.tools = tools; return this; }
    public OrchestrationContext toolExecutor(Function<ToolExecutionRequest, String> executor) { this.toolExecutor = executor; return this; }
    public OrchestrationContext chatModel(ChatModel chatModel) { this.chatModel = chatModel; return this; }

    public OrchestrationContext addMessage(ChatMessage msg) { messages.add(msg); return this; }

    public List<ChatMessage> messages() { return messages; }
    public List<ToolSpecification> tools() { return tools; }
    public Function<ToolExecutionRequest, String> toolExecutor() { return toolExecutor; }
    public ChatModel chatModel() { return chatModel; }
    public String sessionId() { return sessionId; }
    public String model() { return model; }
    public int maxSteps() { return maxSteps; }
    public String systemPrompt() { return systemPrompt; }
}
