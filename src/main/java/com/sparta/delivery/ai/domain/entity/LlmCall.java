package com.sparta.delivery.ai.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "p_llm_calls")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LlmCall {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "call_id")
    private UUID callId;

    @Column(name = "llm_id", nullable = false)
    private UUID llmId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "input_snapshot", nullable = false, columnDefinition = "jsonb")
    private String inputSnapshot;

    @Column(name = "provider_status_code", length = 50)
    private String providerStatusCode;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    @Column(name = "generated_text", columnDefinition = "TEXT")
    private String generatedText;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    public static LlmCall create(
            UUID llmId,
            UUID productId,
            String inputSnapshot,
            String providerStatusCode,
            String rawResponse,
            String generatedText,
            Long createdBy
    ) {
        LlmCall llmCall = new LlmCall();
        llmCall.llmId = llmId;
        llmCall.productId = productId;
        llmCall.inputSnapshot = inputSnapshot;
        llmCall.providerStatusCode = providerStatusCode;
        llmCall.rawResponse = rawResponse;
        llmCall.generatedText = generatedText;
        llmCall.createdAt = LocalDateTime.now();
        llmCall.createdBy = createdBy;
        return llmCall;
    }

}
