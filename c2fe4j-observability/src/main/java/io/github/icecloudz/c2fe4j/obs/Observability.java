package io.github.icecloudz.c2fe4j.obs;

import java.time.Instant;
import java.util.UUID;

/**
 * Factory for creating traces and spans.
 * Usage:
 * <pre>
 *   Observability obs = new Observability();
 *   Trace trace = obs.startTrace(sessionId);
 *   Span span = obs.startSpan(trace.traceId(), "llm_call", "chat_completion");
 *   // ... do work ...
 *   obs.endSpan(span, tokenUsage);
 *   trace.complete();
 * </pre>
 */
public class Observability {

    /**
     * Start a new trace for an agent interaction.
     */
    public Trace startTrace(String sessionId) {
        return new Trace(UUID.randomUUID().toString(), sessionId);
    }

    /**
     * Start a new span within a trace.
     */
    public Span startSpan(String traceId, String type, String name) {
        return new Span(
                UUID.randomUUID().toString(),
                null,
                traceId,
                type,
                name,
                Instant.now(),
                null,
                null,
                "ok",
                null
        );
    }

    /**
     * Start a child span.
     */
    public Span startChildSpan(Span parent, String type, String name) {
        return new Span(
                UUID.randomUUID().toString(),
                parent.spanId(),
                parent.traceId(),
                type,
                name,
                Instant.now(),
                null,
                null,
                "ok",
                null
        );
    }

    /**
     * Complete a span successfully.
     */
    public Span endSpan(Span span, TokenUsage tokenUsage) {
        return new Span(
                span.spanId(),
                span.parentSpanId(),
                span.traceId(),
                span.type(),
                span.name(),
                span.startTime(),
                Instant.now(),
                tokenUsage,
                "ok",
                null
        );
    }

    /**
     * Complete a span with an error.
     */
    public Span endSpanWithError(Span span, String error) {
        return new Span(
                span.spanId(),
                span.parentSpanId(),
                span.traceId(),
                span.type(),
                span.name(),
                span.startTime(),
                Instant.now(),
                null,
                "error",
                error
        );
    }
}
