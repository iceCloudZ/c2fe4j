package io.github.icecloudz.c2fe4j.obs;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A single span in an agent trace.
 * Records one step of the agent loop: LLM call, tool execution, or routing decision.
 */
public record Span(
        String spanId,
        String parentSpanId,
        String traceId,
        String type,
        String name,
        Instant startTime,
        Instant endTime,
        TokenUsage tokenUsage,
        String status,
        String error
) {
    public long durationMs() {
        if (startTime == null || endTime == null) return 0;
        return endTime.toEpochMilli() - startTime.toEpochMilli();
    }
}
