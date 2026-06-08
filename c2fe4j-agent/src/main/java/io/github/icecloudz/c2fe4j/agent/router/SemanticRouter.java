package io.github.icecloudz.c2fe4j.agent.router;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Embedding-based semantic router (P2 Cognitive Gateway).
 *
 * Routes queries by cosine similarity against pre-loaded prototypes.
 * Supports ambiguity detection (gap between top-2 scores) and low-confidence fallback.
 *
 * Prototype data is domain-specific and loaded by the caller via addPrototype().
 * The router itself is domain-agnostic.
 */
public class SemanticRouter {

    private static final Logger log = LoggerFactory.getLogger(SemanticRouter.class);

    private final EmbeddingService embeddingService;
    private final List<Route> routes = new ArrayList<>();
    private double lowConfidenceThreshold = 0.45;
    private double ambiguityGap = 0.08;

    public SemanticRouter(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    public SemanticRouter(EmbeddingService embeddingService, double lowConfidenceThreshold, double ambiguityGap) {
        this.embeddingService = embeddingService;
        this.lowConfidenceThreshold = lowConfidenceThreshold;
        this.ambiguityGap = ambiguityGap;
    }

    /**
     * Route a query against loaded prototypes.
     */
    public RouteResult route(String query) {
        if (routes.isEmpty()) {
            return RouteResult.fallback("no_prototypes");
        }

        float[] queryEmbedding;
        try {
            queryEmbedding = embeddingService.embed(query);
        } catch (Exception e) {
            log.warn("Embedding failed for routing, falling back: {}", e.getMessage());
            return RouteResult.fallback("embedding_failed");
        }

        if (queryEmbedding == null) {
            return RouteResult.fallback("embedding_null");
        }

        double bestScore = -1, secondBestScore = -1;
        Route bestRoute = null;
        for (Route route : routes) {
            double score = cosine(queryEmbedding, route.embedding());
            if (score > bestScore) {
                secondBestScore = bestScore;
                bestScore = score;
                bestRoute = route;
            } else if (score > secondBestScore) {
                secondBestScore = score;
            }
        }

        if (bestRoute == null) {
            return RouteResult.fallback("no_match");
        }

        if (bestScore < lowConfidenceThreshold) {
            return new RouteResult(bestRoute.label(), bestScore, true, "low_confidence");
        }

        boolean ambiguous = bestScore > 0.9 && secondBestScore > 0
                && (bestScore - secondBestScore) < ambiguityGap;

        return new RouteResult(bestRoute.label(), bestScore, ambiguous, ambiguous ? "ambiguous" : "routed");
    }

    /**
     * Add a prototype for routing.
     */
    public void addPrototype(String text, String label) {
        float[] embedding = embeddingService.embed(text);
        if (embedding != null) {
            routes.add(new Route(text, label, embedding));
            log.info("Prototype added: label={}, text={}", label, text);
        }
    }

    public int prototypeCount() { return routes.size(); }

    private double cosine(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0 ? 0 : dot / denom;
    }

    public record Route(String text, String label, float[] embedding) {}
}
