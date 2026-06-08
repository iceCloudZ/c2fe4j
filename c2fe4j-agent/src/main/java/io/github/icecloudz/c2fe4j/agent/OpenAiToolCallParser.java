package io.github.icecloudz.c2fe4j.agent;

import io.github.icecloudz.c2fe4j.core.tool.ToolCall;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for tool calls embedded in LLM response content.
 * Handles the [TOOL_CALL:id:name:args] format from OpenAiLlmClient.
 */
public final class OpenAiToolCallParser {

    private static final Pattern TOOL_CALL_PATTERN = Pattern.compile("\\[TOOL_CALL:([^:]+):([^:]+):(.+?)\\]");

    private OpenAiToolCallParser() {}

    public static List<ToolCall> parse(String content) {
        if (content == null || content.isBlank()) return List.of();
        List<ToolCall> calls = new ArrayList<>();
        Matcher m = TOOL_CALL_PATTERN.matcher(content);
        while (m.find()) {
            calls.add(new ToolCall(m.group(1), m.group(2), m.group(3)));
        }
        return calls;
    }

    public static String strip(String content) {
        if (content == null) return "";
        return TOOL_CALL_PATTERN.matcher(content).replaceAll("").trim();
    }
}
