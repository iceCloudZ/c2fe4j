package io.github.icecloudz.c2fe4j.agent.orchestration;

/**
 * Result from an orchestration strategy execution.
 */
public record OrchestrationResult(
        String reply,
        int totalToolCalls,
        int totalPromptTokens,
        int totalCompletionTokens,
        int llmCallCount,
        String terminationReason,
        long latencyMs
) {
    public static OrchestrationResult error(String message, long latencyMs) {
        return new OrchestrationResult(message, 0, 0, 0, 0, "error", latencyMs);
    }
}
