package io.github.icecloudz.c2fe4j.core;

/**
 * Decision gate for the dual-gateway pattern (P2).
 *
 * Evaluates whether a query needs retrieval, which model to use,
 * and how confident the system is about routing.
 *
 * This interface is designed for pluggable evolution:
 * - Phase 1: rule-based or embedding similarity
 * - Phase 2: LLM self-reflection
 * - Phase 3: native model confidence (when models support it)
 */
@FunctionalInterface
public interface DecisionGate {

    /**
     * Evaluate a query against the current context.
     */
    DecisionResult evaluate(String query, AgentContext context);

    /**
     * Default gate that always allows retrieval (no filtering).
     */
    static DecisionGate permissive() {
        return (query, ctx) -> new DecisionResult(true, false, 1.0f, "permissive");
    }
}
