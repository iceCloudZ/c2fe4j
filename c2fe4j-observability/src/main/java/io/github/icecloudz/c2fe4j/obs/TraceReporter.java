package io.github.icecloudz.c2fe4j.obs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * Collects and reports traces.
 * Supports multiple reporters (logging, storage, external systems).
 */
public class TraceReporter {

    private static final Logger log = LoggerFactory.getLogger(TraceReporter.class);

    private final ConcurrentLinkedQueue<Trace> traces = new ConcurrentLinkedQueue<>();
    private final List<Consumer<Trace>> reporters = new ArrayList<>();
    private final int maxRetention;

    public TraceReporter() {
        this(1000);
    }

    public TraceReporter(int maxRetention) {
        this.maxRetention = maxRetention;
    }

    /**
     * Add a reporter that processes completed traces.
     */
    public TraceReporter addReporter(Consumer<Trace> reporter) {
        reporters.add(reporter);
        return this;
    }

    /**
     * Report a completed trace.
     */
    public void report(Trace trace) {
        traces.add(trace);
        while (traces.size() > maxRetention) {
            traces.poll();
        }
        for (Consumer<Trace> reporter : reporters) {
            try {
                reporter.accept(trace);
            } catch (Exception e) {
                log.warn("Trace reporter failed", e);
            }
        }
    }

    /**
     * Get recent traces for analysis.
     */
    public List<Trace> recentTraces() {
        return List.copyOf(traces);
    }

    /**
     * Add a default logging reporter.
     */
    public TraceReporter withLogging() {
        return addReporter(t -> {
            TokenUsage total = t.aggregateTokens();
            log.info("[Trace:{}] status={} steps={} tokens={}(+{} cached) latency={}ms",
                    t.traceId().substring(0, 8),
                    t.status(),
                    t.spans().size(),
                    total.totalTokens(),
                    total.cachedTokens(),
                    t.durationMs());
        });
    }
}
