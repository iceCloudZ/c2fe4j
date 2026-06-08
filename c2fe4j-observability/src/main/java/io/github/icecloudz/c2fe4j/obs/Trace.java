package io.github.icecloudz.c2fe4j.obs;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A complete trace of an agent interaction.
 * Collects all spans (LLM calls, tool executions, routing decisions)
 * for post-hoc analysis (P6: trace-driven cognitive evolution).
 */
public class Trace {

    private final String traceId;
    private final String sessionId;
    private final Instant startTime;
    private final List<Span> spans = new ArrayList<>();
    private Instant endTime;
    private String status = "ok";

    public Trace(String traceId, String sessionId) {
        this.traceId = traceId;
        this.sessionId = sessionId;
        this.startTime = Instant.now();
    }

    public void addSpan(Span span) {
        spans.add(span);
    }

    public void complete() {
        this.endTime = Instant.now();
    }

    public void fail(String error) {
        this.status = "error: " + error;
        this.endTime = Instant.now();
    }

    /**
     * Aggregate token usage across all spans.
     */
    public TokenUsage aggregateTokens() {
        int prompt = 0, completion = 0, cached = 0, toolCalls = 0;
        for (Span s : spans) {
            if (s.tokenUsage() != null) {
                prompt += s.tokenUsage().promptTokens();
                completion += s.tokenUsage().completionTokens();
                cached += s.tokenUsage().cachedTokens();
                toolCalls += s.tokenUsage().toolCalls();
            }
        }
        return new TokenUsage(prompt, completion, prompt + completion, cached, toolCalls);
    }

    public long durationMs() {
        if (endTime == null) return 0;
        return endTime.toEpochMilli() - startTime.toEpochMilli();
    }

    public String traceId() { return traceId; }
    public String sessionId() { return sessionId; }
    public Instant startTime() { return startTime; }
    public List<Span> spans() { return spans; }
    public String status() { return status; }
}
