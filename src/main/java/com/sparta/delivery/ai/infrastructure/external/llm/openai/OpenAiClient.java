package com.sparta.delivery.ai.infrastructure.external.llm.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.delivery.ai.domain.entity.Llm;
import com.sparta.delivery.ai.domain.entity.LlmProvider;
import com.sparta.delivery.ai.domain.exception.ExternalLlmCallFailedException;
import com.sparta.delivery.ai.infrastructure.external.llm.LlmClient;
import com.sparta.delivery.ai.infrastructure.external.llm.LlmGenerateRequest;
import com.sparta.delivery.ai.infrastructure.external.llm.LlmGenerateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OpenAiClient implements LlmClient {

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(LlmProvider provider) {
        return provider == LlmProvider.OPENAI;
    }

    @Override
    public LlmGenerateResponse generate(Llm llm, LlmGenerateRequest request) {
        try {
            ChatResponse chatResponse = chatClientBuilder.build()
                    .prompt()
                    .options(OpenAiChatOptions.builder()
                            .model(llm.getLlmName())
                            .build())
                    .system("""
                        당신은 음식 배달 플랫폼의 상품 설명 작성 전문가입니다.
                        - 고객이 음식을 주문하고 싶어지도록 매력적으로 작성하세요.
                        - 3문장 이하로 간결하게 작성하세요. 총 길이는 한글 음절수 기준 50자를 넘을 수 없습니다.
                        - 과장된 표현은 피하고 자연스러운 문체를 유지하세요.
                        """)
                    .user(request.prompt())
                    .call()
                    .chatResponse();

            String rawResponse;
            try {
                rawResponse = objectMapper.writeValueAsString(chatResponse);
            } catch (JsonProcessingException e) {
                rawResponse = null;
            }

            String content = chatResponse.getResult().getOutput().getText();

            String finishReason = chatResponse.getResult()
                    .getMetadata()
                    .getFinishReason();

            return new LlmGenerateResponse(
                    content,
                    rawResponse,
                    finishReason
            );
        } catch (Exception e) {
            throw new ExternalLlmCallFailedException();
        }
    }
}
