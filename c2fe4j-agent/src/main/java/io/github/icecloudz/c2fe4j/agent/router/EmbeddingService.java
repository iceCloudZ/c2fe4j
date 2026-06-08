package io.github.icecloudz.c2fe4j.agent.router;

/**
 * Interface for generating embeddings.
 * Implementations connect to specific embedding providers (OpenAI, local models, etc.).
 */
public interface EmbeddingService {

    /**
     * Generate an embedding vector for the given text.
     * @return float array or null on failure
     */
    float[] embed(String text);
}
