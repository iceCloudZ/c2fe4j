package io.github.icecloudz.c2fe4j.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.icecloudz.c2fe4j.core.ChatMessage;
import io.github.icecloudz.c2fe4j.core.ChatResponse;
import io.github.icecloudz.c2fe4j.core.LlmClient;
import io.github.icecloudz.c2fe4j.core.tool.ToolCall;
import io.github.icecloudz.c2fe4j.core.tool.ToolDefinition;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * OpenAI-compatible chat completion client.
 *
 * Works with any OpenAI-compatible endpoint: OpenAI, DeepSeek, Ollama, vLLM, etc.
 * Uses java.net.http.HttpClient (zero external HTTP dependencies).
 */
public class OpenAiLlmClient implements LlmClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;

    public OpenAiLlmClient(String baseUrl, String apiKey, String model) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    public OpenAiLlmClient(String baseUrl, String apiKey, String model, Duration timeout) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
    }

    @Override
    public ChatResponse chat(List<ChatMessage> messages) {
        return chat(messages, List.of());
    }

    @Override
    public ChatResponse chat(List<ChatMessage> messages, List<ToolDefinition> tools) {
        try {
            ObjectNode body = MAPPER.createObjectNode();
            body.put("model", model);
            body.put("stream", false);

            ArrayNode msgs = body.putArray("messages");
            for (ChatMessage msg : messages) {
                ObjectNode m = msgs.addObject();
                m.put("role", msg.role().name().toLowerCase());
                m.put("content", msg.content());
            }

            if (tools != null && !tools.isEmpty()) {
                ArrayNode toolsNode = body.putArray("tools");
                for (ToolDefinition tool : tools) {
                    ObjectNode t = toolsNode.addObject();
                    t.put("type", "function");
                    ObjectNode fn = t.putObject("function");
                    fn.put("name", tool.name());
                    fn.put("description", tool.description());
                    if (tool.parameterSchema() != null && !tool.parameterSchema().isBlank()) {
                        fn.set("parameters", MAPPER.readTree(tool.parameterSchema()));
                    }
                }
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                    .timeout(Duration.ofSeconds(120))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("LLM API error " + response.statusCode() + ": " + response.body());
            }

            JsonNode root = MAPPER.readTree(response.body());
            JsonNode choices = root.path("choices");
            if (choices.isEmpty()) {
                return ChatResponse.of("");
            }

            JsonNode choice = choices.get(0);
            JsonNode message = choice.path("message");
            String content = message.path("content").asText("");
            String finishReason = choice.path("finish_reason").asText("stop");

            // Token usage
            ChatResponse.TokenUsage tokenUsage = parseTokenUsage(root.path("usage"));

            // Tool calls
            JsonNode toolCallsNode = message.path("tool_calls");
            if (!toolCallsNode.isMissingNode() && toolCallsNode.isArray()) {
                // Return tool calls as metadata — the caller (AgentLoop) handles execution
                StringBuilder sb = new StringBuilder(content);
                for (JsonNode tc : toolCallsNode) {
                    String tcId = tc.path("id").asText();
                    String tcName = tc.path("function").path("name").asText();
                    String tcArgs = tc.path("function").path("arguments").asText();
                    sb.append("\n[TOOL_CALL:").append(tcId).append(":").append(tcName).append(":").append(tcArgs).append("]");
                }
                content = sb.toString();
            }

            return new ChatResponse(content, tokenUsage, finishReason);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("LLM call failed", e);
        }
    }

    /**
     * Parse a response that may contain tool calls from the mixed content format.
     */
    public static List<ToolCall> parseToolCalls(String content) {
        return java.util.regex.Pattern.compile("\\[TOOL_CALL:([^:]+):([^:]+):(.+?)\\]")
                .matcher(content)
                .results()
                .map(m -> new ToolCall(m.group(1), m.group(2), m.group(3)))
                .toList();
    }

    /**
     * Extract plain content without tool call markers.
     */
    public static String stripToolCalls(String content) {
        return java.util.regex.Pattern.compile("\\n?\\[TOOL_CALL:[^\\]]+\\]").matcher(content).replaceAll("");
    }

    @Override
    public String getModel() {
        return model;
    }

    private ChatResponse.TokenUsage parseTokenUsage(JsonNode usage) {
        if (usage.isMissingNode() || usage.isNull()) {
            return ChatResponse.TokenUsage.empty();
        }
        return new ChatResponse.TokenUsage(
                usage.path("prompt_tokens").asInt(0),
                usage.path("completion_tokens").asInt(0),
                usage.path("total_tokens").asInt(0),
                usage.path("prompt_tokens_details").path("cached_tokens").asInt(0)
        );
    }
}
