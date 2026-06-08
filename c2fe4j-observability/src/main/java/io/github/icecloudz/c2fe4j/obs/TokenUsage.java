package io.github.icecloudz.c2fe4j.obs;

/**
 * Token usage for a single LLM call.
 */
public record TokenUsage(
        int promptTokens,
        int completionTokens,
        int totalTokens,
        int cachedTokens,
        int toolCalls
) {
    public static TokenUsage empty() {
        return new TokenUsage(0, 0, 0, 0, 0);
    }

    public static TokenUsage of(int prompt, int completion) {
        return new TokenUsage(prompt, completion, prompt + completion, 0, 0);
    }
}
