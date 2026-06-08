package io.github.icecloudz.c2fe4j.core;

import io.github.icecloudz.c2fe4j.core.tool.ToolDefinition;

import java.util.List;
import java.util.Optional;

/**
 * Abstraction over LLM chat completion APIs.
 * Implementations handle specific providers (OpenAI, DeepSeek, Ollama, etc.).
 */
public interface LlmClient {

    /**
     * Non-streaming chat completion.
     */
    ChatResponse chat(List<ChatMessage> messages);

    /**
     * Chat completion with tool definitions available.
     * The LLM may or may not invoke tools (P3: LLM decides).
     */
    ChatResponse chat(List<ChatMessage> messages, List<ToolDefinition> tools);

    /**
     * Get the model identifier.
     */
    String getModel();
}
