package io.github.icecloudz.c2fe4j.core;

/**
 * Final result of an agent loop run.
 */
public record AgentResult(
        String content,
        AgentContext context,
        ChatResponse.TokenUsage tokenUsage,
        int steps,
        long latencyMs
) {}
