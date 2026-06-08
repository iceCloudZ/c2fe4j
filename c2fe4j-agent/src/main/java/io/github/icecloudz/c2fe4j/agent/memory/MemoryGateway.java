package io.github.icecloudz.c2fe4j.agent.memory;

import java.util.List;
import java.util.Map;

/**
 * Interface for multi-layer memory management (P1 cognitive bandwidth).
 *
 * Three memory layers:
 * - Semantic: user facts organized by category (e.g., preferences, constraints)
 * - Episodic: session summaries with temporal context
 * - Procedural: communication style preferences and interaction patterns
 *
 * Implementations choose their own storage (PostgreSQL+pgvector, SQLite, in-memory, etc.).
 */
public interface MemoryGateway {

    /**
     * Build a memory directive string to inject into LLM context.
     * Combines relevant memories from all layers.
     */
    String buildMemoryDirective(String sessionId, String userId);

    /**
     * Load working memory for the current session.
     * Returns key-value pairs of active memories.
     */
    Map<String, String> loadWorkingMemory(String sessionId, String userId);

    /**
     * Extract and save memories from a completed conversation.
     */
    void extractAndSaveMemories(String sessionId, String userId, String conversation);

    /**
     * Recall semantically similar memories for a query.
     */
    List<MemoryEntry> recall(String query, String userId, int limit);

    /**
     * A single memory entry.
     */
    record MemoryEntry(
            String category,
            String content,
            double relevance,
            String source
    ) {}
}
