package io.github.icecloudz.c2fe4j.core;

/**
 * Result from a DecisionGate evaluation (P2).
 */
public record DecisionResult(
        boolean needRetrieval,
        boolean needProModel,
        float confidence,
        String reasoning
) {}
