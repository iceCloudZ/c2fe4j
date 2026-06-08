package io.github.icecloudz.c2fe4j.agent;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.github.icecloudz.c2fe4j.obs.Observability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Multi-domain routing agent (ButlerAgent pattern).
 *
 * Routes queries to appropriate domain agents, retrieves their context,
 * and lets the LLM answer with full domain knowledge.
 *
 * P2 (dual gateway): routing is the cognitive gate, context retrieval is the data gate.
 */
public class RouterAgent {

    private static final Logger log = LoggerFactory.getLogger(RouterAgent.class);

    private final ChatModel chatModel;
    private final Map<String, DomainAgent> domainAgents;
    private final Observability observability;
    private final String routerPrompt;

    public RouterAgent(ChatModel chatModel,
                       List<DomainAgent> agents,
                       Observability observability) {
        this.chatModel = chatModel;
        this.domainAgents = agents.stream()
                .collect(Collectors.toMap(DomainAgent::domain, a -> a));
        this.observability = observability;
        this.routerPrompt = buildRouterPrompt();
    }

    /**
     * Answer a user query by routing to appropriate domains.
     */
    public String answer(String query, String sessionId, String systemPromptBase) {
        List<String> domains = route(query);

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

        String systemPrompt = systemPromptBase;
        if (!contextBuilder.isEmpty()) {
            systemPrompt += "\n\n## 当前家庭数据\n\n" + contextBuilder;
        }

        ChatResponse response = chatModel.chat(
                SystemMessage.from(systemPrompt),
                UserMessage.from(query)
        );

        log.info("RouterAgent response: tokens={}", response.tokenUsage());
        return response.aiMessage().text();
    }

    private List<String> route(String query) {
        if (domainAgents.isEmpty()) return List.of();

        try {
            String domainList = String.join(", ", domainAgents.keySet());
            String prompt = routerPrompt.formatted(domainList, query);

            ChatResponse response = chatModel.chat(UserMessage.from(prompt));
            String[] parts = response.aiMessage().text().split("[,，\\s]+");
            List<String> result = new ArrayList<>();
            for (String part : parts) {
                String trimmed = part.trim().toLowerCase();
                if (domainAgents.containsKey(trimmed)) {
                    result.add(trimmed);
                }
            }

            if (result.isEmpty()) {
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
