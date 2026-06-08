package io.github.icecloudz.c2fe4j.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.icecloudz.c2fe4j.core.ChatMessage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SSE streaming client for OpenAI-compatible chat completions.
 * Emits content chunks as they arrive.
 */
public class StreamingChatClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern SSE_DATA = Pattern.compile("^data:\\s*(.+)$");

    private final String baseUrl;
    private final String apiKey;
    private final HttpClient httpClient;

    public StreamingChatClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * Stream a chat completion, calling the consumer for each content chunk.
     * Returns the full accumulated response.
     */
    public String streamChat(List<ChatMessage> messages, String model, Consumer<String> onChunk) {
        try {
            ObjectNode body = MAPPER.createObjectNode();
            body.put("model", model);
            body.put("stream", true);

            ArrayNode msgs = body.putArray("messages");
            for (ChatMessage msg : messages) {
                ObjectNode m = msgs.addObject();
                m.put("role", msg.role().name().toLowerCase());
                m.put("content", msg.content());
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                    .timeout(Duration.ofSeconds(180))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            StringBuilder full = new StringBuilder();
            String[] lines = response.body().split("\n");
            for (String line : lines) {
                Matcher m = SSE_DATA.matcher(line.trim());
                if (!m.matches()) continue;
                String data = m.group(1);
                if ("[DONE]".equals(data)) break;

                JsonNode chunk = MAPPER.readTree(data);
                JsonNode delta = chunk.path("choices").path(0).path("delta");
                String content = delta.path("content").asText("");
                if (!content.isEmpty()) {
                    full.append(content);
                    onChunk.accept(content);
                }
            }

            return full.toString();
        } catch (Exception e) {
            throw new RuntimeException("Streaming LLM call failed", e);
        }
    }
}
