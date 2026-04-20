package com.sparta.delivery.ai.domain.entity;


import com.sparta.delivery.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "p_llms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Llm extends BaseEntity {

    @Id
    @Column(name = "llm_id")
    private UUID llmId;

    @Column(name = "llm_name", nullable = false, unique = true, length = 100)
    private String llmName;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    public static Llm create(UUID llmId, String llmName, boolean isActive) {
        validateLlmName(llmName);

        Llm llm = new Llm();
        llm.llmId = llmId;
        llm.llmName = llmName;
        llm.isActive = isActive;

        return llm;
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

    // not null, non-blank and max length 100
    private static void validateLlmName(String llmName) {
        if (llmName == null || llmName.isBlank() || llmName.length() > 100) {
            throw new IllegalArgumentException("Invalid model name.");
        }
    }
}
