package io.github.icecloudz.c2fe4j.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for c2fe4j.
 *
 * <pre>
 * c2fe4j:
 *   llm:
 *     base-url: https://api.deepseek.com
 *     api-key: sk-xxx
 *     model: deepseek-chat
 *   agent:
 *     max-steps: 10
 *     system-prompt: "You are a helpful assistant."
 * </pre>
 */
@ConfigurationProperties(prefix = "c2fe4j")
public record C2fe4jProperties(
        LlmConfig llm,
        AgentConfig agent,
        ObservabilityConfig observability
) {
    public C2fe4jProperties() {
        this(new LlmConfig(), new AgentConfig(), new ObservabilityConfig());
    }

    public record LlmConfig(
            String baseUrl,
            String apiKey,
            String model
    ) {
        public LlmConfig() {
            this("https://api.deepseek.com", "", "deepseek-chat");
        }
    }

    public record AgentConfig(
            int maxSteps,
            String systemPrompt
    ) {
        public AgentConfig() {
            this(10, "You are a helpful assistant.");
        }
    }

    public record ObservabilityConfig(
            boolean enabled,
            int traceRetention
    ) {
        public ObservabilityConfig() {
            this(true, 1000);
        }
    }
}
