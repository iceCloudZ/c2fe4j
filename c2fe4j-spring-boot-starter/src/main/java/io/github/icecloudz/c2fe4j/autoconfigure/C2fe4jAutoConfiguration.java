package io.github.icecloudz.c2fe4j.autoconfigure;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import io.github.icecloudz.c2fe4j.agent.DomainAgent;
import io.github.icecloudz.c2fe4j.agent.ReActAgentLoop;
import io.github.icecloudz.c2fe4j.agent.RouterAgent;
import io.github.icecloudz.c2fe4j.obs.Observability;
import io.github.icecloudz.c2fe4j.obs.TraceReporter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.time.Duration;
import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties(C2fe4jProperties.class)
public class C2fe4jAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ChatModel.class)
    public ChatModel chatModel(C2fe4jProperties props) {
        C2fe4jProperties.LlmConfig llm = props.llm();
        return OpenAiChatModel.builder()
                .baseUrl(llm.baseUrl())
                .apiKey(llm.apiKey())
                .modelName(llm.model())
                .timeout(Duration.ofSeconds(llm.timeoutSeconds()))
                .maxRetries(llm.maxRetries())
                .build();
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
        return new TraceReporter(props.observability().traceRetention()).withLogging();
    }

    @Bean
    @ConditionalOnMissingBean
    public ReActAgentLoop reActAgentLoop(ChatModel chatModel,
                                         C2fe4jProperties props,
                                         Observability observability) {
        return new ReActAgentLoop(chatModel, List.of(), null, observability, props.agent().maxSteps());
    }

    @Bean
    @ConditionalOnMissingBean
    public RouterAgent routerAgent(ChatModel chatModel,
                                   List<DomainAgent> domainAgents,
                                   Observability observability) {
        return new RouterAgent(chatModel, domainAgents, observability);
    }
}
