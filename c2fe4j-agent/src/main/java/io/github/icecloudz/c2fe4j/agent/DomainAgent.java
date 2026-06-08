package io.github.icecloudz.c2fe4j.agent;

import io.github.icecloudz.c2fe4j.core.ChatMessage;

import java.util.List;

/**
 * A domain-specific agent that can retrieve context and provide a system prompt.
 * Used by RouterAgent to delegate to specialized agents.
 */
public interface DomainAgent {

    /**
     * The domain this agent handles (e.g., "finance", "health", "work").
     */
    String domain();

    /**
     * System prompt for this domain.
     */
    String systemPrompt();

    /**
     * Retrieve relevant context for the given query.
     * Returns context to be injected into the conversation (P1: high SNR data).
     */
    String retrieveContext(String query);
}
