package io.github.icecloudz.c2fe4j.agent;

/**
 * A domain-specific agent that can retrieve context and provide a system prompt.
 * Used by RouterAgent to delegate to specialized agents.
 */
public interface DomainAgent {

    /** The domain this agent handles (e.g., "finance", "health", "work"). */
    String domain();

    /** System prompt for this domain. */
    String systemPrompt();

    /** Retrieve relevant context for the given query (P1: high SNR data). */
    String retrieveContext(String query);
}
