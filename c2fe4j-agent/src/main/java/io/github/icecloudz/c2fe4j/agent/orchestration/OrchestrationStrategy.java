package io.github.icecloudz.c2fe4j.agent.orchestration;

/**
 * Orchestration strategy interface for agent loops.
 *
 * Implementations define how an agent processes a query:
 * - ReAct: step-by-step observe→think→act
 * - PlanExecute: plan tasks upfront, execute in parallel, synthesize
 *
 * Strategy selection can be dynamic (A/B testing, complexity-based routing).
 */
public interface OrchestrationStrategy {

    /**
     * Execute the agent loop and return the final result.
     */
    OrchestrationResult execute(OrchestrationContext context);

    /**
     * Strategy identifier for logging and experimentation.
     */
    String name();
}
