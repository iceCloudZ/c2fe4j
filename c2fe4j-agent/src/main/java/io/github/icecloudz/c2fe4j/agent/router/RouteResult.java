package io.github.icecloudz.c2fe4j.agent.router;

/**
 * Result from SemanticRouter.route().
 */
public record RouteResult(
        String label,
        double score,
        boolean fallback,
        String reason
) {
    public static RouteResult fallback(String reason) {
        return new RouteResult("need_rag", 0, true, reason);
    }
}
