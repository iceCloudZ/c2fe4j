package io.github.icecloudz.c2fe4j.core.tool;

/**
 * A tool call requested by the LLM.
 */
public record ToolCall(
        String id,
        String name,
        String arguments
) {}
