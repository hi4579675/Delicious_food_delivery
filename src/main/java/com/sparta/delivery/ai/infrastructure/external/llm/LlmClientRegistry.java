package com.sparta.delivery.ai.infrastructure.external.llm;

import com.sparta.delivery.ai.domain.entity.LlmProvider;
import com.sparta.delivery.ai.domain.exception.ExternalLlmCallFailedException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LlmClientRegistry {

    private final List<LlmClient> clients;

    public LlmClientRegistry(List<LlmClient> clients) {
        this.clients = clients;
    }

    public LlmClient getClient(LlmProvider provider) {
        return clients.stream()
                .filter(client -> client.supports(provider))
                .findFirst()
                .orElseThrow(ExternalLlmCallFailedException::new);
    }
}
