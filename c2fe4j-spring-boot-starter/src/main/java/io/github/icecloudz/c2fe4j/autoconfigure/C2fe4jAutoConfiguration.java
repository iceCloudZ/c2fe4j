package io.github.icecloudz.c2fe4j.autoconfigure;

import io.github.icecloudz.c2fe4j.agent.ReActAgentLoop;
import io.github.icecloudz.c2fe4j.agent.RouterAgent;
import io.github.icecloudz.c2fe4j.core.*;
import io.github.icecloudz.c2fe4j.core.tool.ToolDefinition;
import io.github.icecloudz.c2fe4j.core.tool.ToolExecutor;
import io.github.icecloudz.c2fe4j.obs.Observability;
import io.github.icecloudz.c2fe4j.obs.TraceReporter;
import io.github.icecloudz.c2fe4j.openai.CostCalculator;
import io.github.icecloudz.c2fe4j.openai.OpenAiLlmClient;
import io.github.icecloudz.c2fe4j.openai.StreamingChatClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.Map;

@AutoConfiguration
@EnableConfigurationProperties(C2fe4jProperties.class)
public class C2fe4jAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LlmClient llmClient(C2fe4jProperties props) {
        C2fe4jProperties.LlmConfig llm = props.llm();
        return new OpenAiLlmClient(llm.baseUrl(), llm.apiKey(), llm.model());
    }

    @Bean
    @ConditionalOnMissingBean
    public StreamingChatClient streamingChatClient(C2fe4jProperties props) {
        return new StreamingChatClient(props.llm().baseUrl(), props.llm().apiKey());
    }

    @Bean
    @ConditionalOnMissingBean
    public CostCalculator costCalculator() {
        return new CostCalculator();
    }

    @Bean
    @ConditionalOnMissingBean
    public Observability observability() {
        return new Observability();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "c2fe4j.observability", name = "enabled", havingValue = "true", matchIfMissing = true)
    public TraceReporter traceReporter(C2fe4jProperties props) {
        TraceReporter reporter = new TraceReporter(props.observability().traceRetention());
        return reporter.withLogging();
    }

    @Bean
    @ConditionalOnMissingBean
    public DecisionGate decisionGate() {
        return DecisionGate.permissive();
    }

    @Bean
    @ConditionalOnMissingBean
    public ReActAgentLoop agentLoop(LlmClient llmClient,
                                    C2fe4jProperties props,
                                    Observability observability) {
        return new ReActAgentLoop(
                llmClient,
                List.of(),
                Map.of(),
                decisionGate(),
                observability,
                props.agent().maxSteps()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public RouterAgent routerAgent(LlmClient llmClient,
                                   List<io.github.icecloudz.c2fe4j.agent.DomainAgent> domainAgents,
                                   Observability observability) {
        return new RouterAgent(llmClient, domainAgents, observability);
    }
}
