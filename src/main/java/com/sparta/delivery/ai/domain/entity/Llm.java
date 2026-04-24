package com.sparta.delivery.ai.domain.entity;


import com.sparta.delivery.ai.domain.exception.InvalidLlmNameException;
import com.sparta.delivery.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(name = "p_llms")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Llm extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "llm_id")
    private UUID llmId;

    @Column(name = "llm_name", nullable = false, unique = true, length = 100)
    private String llmName;

    @Column(name = "provider", nullable = false)
    @Enumerated(EnumType.STRING)
    private LlmProvider provider;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Builder
    private Llm(
            String llmName,
            LlmProvider provider,
            boolean isActive
    ) {
        this.llmName = llmName;
        this.provider = provider;
        this.isActive = isActive;
    }

    public static Llm create(
            String llmName,
            LlmProvider provider,
            boolean isActive
    ) {
        validateLlmName(llmName);

        return Llm.builder()
                .llmName(llmName)
                .provider(provider)
                .isActive(isActive)
                .build();
    }

    public void updateName(String llmName) {
        validateLlmName(llmName);
        this.llmName = llmName;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    // max length 100
    private static void validateLlmName(String llmName) {
        if (llmName != null && llmName.length() > 100) {
            throw new InvalidLlmNameException();
        }
    }
}
