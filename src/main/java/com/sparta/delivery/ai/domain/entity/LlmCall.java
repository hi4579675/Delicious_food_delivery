package com.sparta.delivery.ai.domain.entity;

import com.sparta.delivery.ai.domain.exception.InvalidCreatedByException;
import com.sparta.delivery.ai.domain.exception.InvalidInputSnapshotException;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "p_llm_calls",
        indexes = {
                @Index(name = "idx_llm_calls_llm_id", columnList = "llm_id"),
                @Index(name = "idx_llm_calls_created_at", columnList = "created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LlmCall {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "call_id")
    private UUID callId;

    @Column(name = "llm_id", nullable = false)
    private UUID llmId;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "input_snapshot", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String inputSnapshot;

    @Column(name = "finish_reason", length = 50)
    private String finishReason;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    @Column(name = "generated_text", columnDefinition = "TEXT")
    private String generatedText;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Builder(access = AccessLevel.PRIVATE)
    private LlmCall(
            UUID llmId,
            String inputSnapshot,
            String finishReason,
            String rawResponse,
            String generatedText,
            LocalDateTime createdAt,
            Long createdBy
    ) {
        this.llmId = llmId;
        this.inputSnapshot = inputSnapshot;
        this.finishReason = finishReason;
        this.rawResponse = rawResponse;
        this.generatedText = generatedText;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public static LlmCall create(
            UUID llmId,
            String inputSnapshot,
            String finishReason,
            String rawResponse,
            String generatedText,
            Long createdBy
    ) {
        validateInputSnapshot(inputSnapshot);
        validateCreatedBy(createdBy);

        return LlmCall.builder()
                .llmId(llmId)
                .inputSnapshot(inputSnapshot)
                .finishReason(finishReason)
                .rawResponse(rawResponse)
                .generatedText(generatedText)
                .createdAt(LocalDateTime.now())
                .createdBy(createdBy)
                .build();
    }

    private static void validateInputSnapshot(String inputSnapshot) {
        if (inputSnapshot == null || inputSnapshot.isBlank()) {
            throw new InvalidInputSnapshotException();
        }
    }

    private static void validateCreatedBy(Long createdBy) {
        if (createdBy == null) {
            throw new InvalidCreatedByException();
        }
    }

}
