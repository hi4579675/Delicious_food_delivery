package com.sparta.delivery.ai.infrastructure.external.llm.gemini;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.delivery.ai.domain.entity.LlmProvider;
import com.sparta.delivery.ai.domain.exception.ExternalLlmCallFailedException;
import com.sparta.delivery.ai.domain.vo.ActiveLlmInfo;
import com.sparta.delivery.ai.infrastructure.external.llm.LlmClient;
import com.sparta.delivery.ai.infrastructure.external.llm.LlmGenerateRequest;
import com.sparta.delivery.ai.infrastructure.external.llm.LlmGenerateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GeminiClient implements LlmClient {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public GeminiClient(GoogleGenAiChatModel chatModel, ObjectMapper objectMapper) {
        this.chatClient = ChatClient.create(chatModel);
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(LlmProvider provider) {
        return provider == LlmProvider.GOOGLE;
    }

    @Override
    public LlmGenerateResponse generate(ActiveLlmInfo llm, LlmGenerateRequest request) {
        try {
            ChatResponse chatResponse = chatClient
                    .prompt()
                    .options(GoogleGenAiChatOptions.builder()
                            .model(llm.llmName())
//                            .maxOutputTokens(40)
                            .build())
                    .system("""
                        당신은 음식 배달 플랫폼의 상품 설명 작성 전문가입니다.
                        - 고객이 음식을 주문하고 싶어지도록 매력적으로 작성하세요.
                        - 3문장 이하로 간결하게 작성하세요. 총 길이는 한글 음절수 기준 50자를 넘을 수 없습니다.
                        - 줄바꿈("\n") 없이 한 줄 안에 상품 설명을 완성하세요.
                        - 과장된 표현은 피하고 자연스러운 문체를 유지하세요.
                        """)
                    .user(request.prompt())
                    .call()
                    .chatResponse();

            String rawResponse;
            try {
                rawResponse = objectMapper.writeValueAsString(chatResponse);
            } catch (JsonProcessingException e) {
                log.warn("[Gemini Client] ChatResponse Serialization 실패: model={}, error={}",
                        llm.llmName(), e.getMessage());
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
