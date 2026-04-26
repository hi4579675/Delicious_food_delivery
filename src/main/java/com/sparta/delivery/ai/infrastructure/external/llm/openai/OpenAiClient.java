package com.sparta.delivery.ai.infrastructure.external.llm.openai;

import com.sparta.delivery.ai.domain.entity.Llm;
import com.sparta.delivery.ai.domain.entity.LlmProvider;
import com.sparta.delivery.ai.domain.exception.ExternalLlmCallFailedException;
import com.sparta.delivery.ai.infrastructure.external.llm.LlmClient;
import com.sparta.delivery.ai.infrastructure.external.llm.LlmGenerateRequest;
import com.sparta.delivery.ai.infrastructure.external.llm.LlmGenerateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OpenAiClient implements LlmClient {

    private final ChatClient.Builder chatClientBuilder;

    @Override
    public boolean supports(LlmProvider provider) {
        return provider == LlmProvider.OPENAI;
    }

    @Override
    public LlmGenerateResponse generate(Llm llm, LlmGenerateRequest request) {
        try {
            String content = chatClientBuilder.build()
                    .prompt()
                    .options(OpenAiChatOptions.builder()
                            .model(llm.getLlmName())
                            .build())
                    .user(request.prompt())
                    .call()
                    .content();

            return new LlmGenerateResponse(
                    content,
                    null,
                    "200"
            );
        } catch (Exception e) {
            throw new ExternalLlmCallFailedException();
        }
    }
}
