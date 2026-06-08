package io.github.icecloudz.c2fe4j.agent;

import io.github.icecloudz.c2fe4j.core.*;
import io.github.icecloudz.c2fe4j.obs.Observability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Multi-domain routing agent (ButlerAgent pattern).
 *
 * Routes queries to the appropriate domain agents, retrieves their context,
 * and lets the LLM answer with full domain knowledge.
 *
 * Implements P2 (dual gateway): routing is the cognitive gate,
 * context retrieval is the data gate.
 */
public class RouterAgent {

    private static final Logger log = LoggerFactory.getLogger(RouterAgent.class);

    private final LlmClient llmClient;
    private final Map<String, DomainAgent> domainAgents;
    private final Observability observability;
    private final String routerPrompt;

    public RouterAgent(LlmClient llmClient,
                       List<DomainAgent> agents,
                       Observability observability) {
        this.llmClient = llmClient;
        this.domainAgents = agents.stream()
                .collect(Collectors.toMap(DomainAgent::domain, a -> a));
        this.observability = observability;
        this.routerPrompt = buildRouterPrompt();
    }

    /**
     * Answer a user query by routing to appropriate domains.
     */
    public AgentResult answer(String query, String sessionId, String systemPromptBase) {
        long startTime = System.currentTimeMillis();

        // Step 1: Route to relevant domains
        List<String> domains = route(query);

        // Step 2: Retrieve context from each domain
        StringBuilder contextBuilder = new StringBuilder();
        for (String domain : domains) {
            DomainAgent agent = domainAgents.get(domain);
            if (agent != null) {
                String ctx = agent.retrieveContext(query);
                if (ctx != null && !ctx.isBlank()) {
                    contextBuilder.append("## ").append(domain).append(" 数据\n").append(ctx).append("\n\n");
                }
            }
        }

        // Step 3: Build messages with system prompt + context + query
        AgentContext context = new AgentContext(sessionId);
        String systemPrompt = systemPromptBase;
        if (!contextBuilder.isEmpty()) {
            systemPrompt += "\n\n## 当前家庭数据\n\n" + contextBuilder;
        }
        context.addMessage(ChatMessage.system(systemPrompt));
        context.addMessage(ChatMessage.user(query));

        // Step 4: Get LLM response
        ChatResponse response = llmClient.chat(context.messages());
        context.addMessage(ChatMessage.assistant(response.content()));
        context.addTokens(response.tokenUsage().totalTokens());

        long latency = System.currentTimeMillis() - startTime;
        return new AgentResult(response.content(), context, response.tokenUsage(), 1, latency);
    }

    /**
     * Route a query to relevant domains using LLM.
     */
    private List<String> route(String query) {
        if (domainAgents.isEmpty()) return List.of();

        try {
            String domainList = String.join(", ", domainAgents.keySet());
            String prompt = routerPrompt.formatted(domainList, query);

            ChatResponse response = llmClient.chat(List.of(ChatMessage.user(prompt)));
            String[] parts = response.content().split("[,，\\s]+");
            List<String> result = new ArrayList<>();
            for (String part : parts) {
                String trimmed = part.trim().toLowerCase();
                if (domainAgents.containsKey(trimmed)) {
                    result.add(trimmed);
                }
            }

            if (result.isEmpty()) {
                // If no domain matched, try all domains (conservative)
                result.addAll(domainAgents.keySet());
            }

            log.info("Routed query to domains: {}", result);
            return result;
        } catch (Exception e) {
            log.warn("Routing failed, using all domains", e);
            return List.copyOf(domainAgents.keySet());
        }
    }

    private String buildRouterPrompt() {
        return """
                分析以下问题涉及哪些领域。可用的领域有：%s。
                只返回相关的领域名称，用逗号分隔。不要解释。
                如果不确定，返回所有可能相关的领域。

                问题：%s
                """;
    }
}
