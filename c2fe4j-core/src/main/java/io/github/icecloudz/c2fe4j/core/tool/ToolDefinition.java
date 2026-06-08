package io.github.icecloudz.c2fe4j.core.tool;

/**
 * Definition of a tool that an LLM can invoke.
 *
 * Tools describe their capabilities to the LLM (P3: transparent authorization).
 * The LLM decides when and how to use them — no micro-management.
 */
public record ToolDefinition(
        String name,
        String description,
        String parameterSchema
) {
    public static ToolDefinition of(String name, String description, String parameterSchema) {
        return new ToolDefinition(name, description, parameterSchema);
    }
}
