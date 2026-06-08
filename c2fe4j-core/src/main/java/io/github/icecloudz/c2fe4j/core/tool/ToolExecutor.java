package io.github.icecloudz.c2fe4j.core.tool;

/**
 * Interface for executing tool calls.
 * Implementations handle the actual invocation of domain-specific logic.
 */
@FunctionalInterface
public interface ToolExecutor {
    ToolResult execute(ToolCall call);
}
