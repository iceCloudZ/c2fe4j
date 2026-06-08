package io.github.icecloudz.c2fe4j.core;

/**
 * Agent loop interface (ReAct pattern).
 *
 * Orchestrates the observe → think → act cycle.
 * The loop respects P3 (LLM decides when to stop, not hard-coded limits)
 * and P5 (transparent about system boundaries).
 */
public interface AgentLoop {

    /**
     * Run the agent loop to completion.
     * Returns the final response.
     */
    AgentResult run(AgentContext context);

    /**
     * Run a single step of the agent loop.
     * Useful for streaming or interactive scenarios.
     */
    StepResult step(AgentContext context);

    /**
     * Result of a single step.
     */
    record StepResult(boolean finished, ChatResponse response) {
        public static StepResult ongoing(ChatResponse response) {
            return new StepResult(false, response);
        }

        public static StepResult done(ChatResponse response) {
            return new StepResult(true, response);
        }
    }
}
