package io.github.icecloudz.c2fe4j.core.tool;

/**
 * Result of executing a tool call.
 *
 * Per P5: results are transparent — never fake errors.
 * If a tool is rate-limited, say so honestly so the LLM can plan around it.
 */
public record ToolResult(
        String toolCallId,
        String content,
        boolean isError
) {
    public static ToolResult success(String toolCallId, String content) {
        return new ToolResult(toolCallId, content, false);
    }

    public static ToolResult error(String toolCallId, String honestMessage) {
        return new ToolResult(toolCallId, honestMessage, true);
    }
}
