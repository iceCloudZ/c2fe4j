package io.github.icecloudz.c2fe4j.openai;

import io.github.icecloudz.c2fe4j.core.ChatResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Cost calculator for LLM API calls.
 * Supports configurable pricing per model per million tokens.
 */
public class CostCalculator {

    // Default pricing: model → (prompt price per M tokens, completion price per M tokens)
    private static final Map<String, BigDecimal[]> DEFAULT_PRICING = Map.of(
            "deepseek-chat", new BigDecimal[]{new BigDecimal("1"), new BigDecimal("2")},
            "deepseek-reasoner", new BigDecimal[]{new BigDecimal("4"), new BigDecimal("16")},
            "gpt-4o", new BigDecimal[]{new BigDecimal("2.5"), new BigDecimal("10")},
            "gpt-4o-mini", new BigDecimal[]{new BigDecimal("0.15"), new BigDecimal("0.6")},
            "qwen-plus", new BigDecimal[]{new BigDecimal("0.8"), new BigDecimal("2")},
            "qwen-turbo", new BigDecimal[]{new BigDecimal("0.3"), new BigDecimal("0.6")}
    );

    private final Map<String, BigDecimal[]> pricing;

    public CostCalculator() {
        this(DEFAULT_PRICING);
    }

    public CostCalculator(Map<String, BigDecimal[]> pricing) {
        this.pricing = pricing;
    }

    /**
     * Calculate cost in yuan for a chat response.
     */
    public BigDecimal calculate(String model, ChatResponse.TokenUsage usage) {
        BigDecimal[] prices = pricing.getOrDefault(model, new BigDecimal[]{BigDecimal.ONE, BigDecimal.ONE});
        BigDecimal promptCost = prices[0].multiply(BigDecimal.valueOf(usage.promptTokens())).divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP);
        BigDecimal completionCost = prices[1].multiply(BigDecimal.valueOf(usage.completionTokens())).divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP);
        return promptCost.add(completionCost).setScale(6, RoundingMode.HALF_UP);
    }
}
