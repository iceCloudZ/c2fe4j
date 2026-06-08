package io.github.icecloudz.c2fe4j.core;

/**
 * Response from an LLM chat completion.
 */
public record ChatResponse(
        String content,
        TokenUsage tokenUsage,
        String finishReason
) {

    public record TokenUsage(
            int promptTokens,
            int completionTokens,
            int totalTokens,
            int cachedTokens
    ) {
        public static TokenUsage empty() {
            return new TokenUsage(0, 0, 0, 0);
        }
    }

    public static ChatResponse of(String content) {
        return new ChatResponse(content, TokenUsage.empty(), "stop");
    }
}
