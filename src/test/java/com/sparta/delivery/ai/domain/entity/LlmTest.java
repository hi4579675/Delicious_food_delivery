package com.sparta.delivery.ai.domain.entity;

import com.sparta.delivery.ai.domain.exception.AiErrorCode;
import com.sparta.delivery.ai.domain.exception.InvalidLlmNameException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class LlmTest {

    @Test
    @DisplayName("create should create llm successfully")
    void create_shouldCreateLlmSuccessfully() {
        // given
        String llmName = "gemini-1.5-flash";
        boolean isActive = true;

        // when
        Llm llm = Llm.create(llmName, isActive);

        // then
        assertThat(llm.getLlmName()).isEqualTo(llmName);
        assertThat(llm.isActive()).isTrue();
    }

    @Test
    @DisplayName("create should throw when llmName length exceeds 100")
    void create_shouldThrow_whenLlmNameLengthExceeds100() {
        // given
        String llmName = "a".repeat(101);

        // when
        Throwable thrown = catchThrowable(() -> Llm.create(llmName, false));

        // then
        assertThat(thrown).isInstanceOf(InvalidLlmNameException.class);
        InvalidLlmNameException exception = (InvalidLlmNameException) thrown;
        assertThat(exception.getCode()).isEqualTo(AiErrorCode.INVALID_LLM_NAME.getCode());
    }

    @Test
    @DisplayName("updateName should change llmName successfully")
    void updateName_shouldChangeLlmNameSuccessfully() {
        // given
        Llm llm = Llm.create("gemini-1.5-flash", false);
        String updatedName = "gemini-2.0-flash";

        // when
        llm.updateName(updatedName);

        // then
        assertThat(llm.getLlmName()).isEqualTo(updatedName);
    }

    @Test
    @DisplayName("updateName should throw when llmName length exceeds 100")
    void updateName_shouldThrow_whenLlmNameLengthExceeds100() {
        // given
        Llm llm = Llm.create("gemini-1.5-flash", false);

        // when
        Throwable thrown = catchThrowable(() -> llm.updateName("a".repeat(101)));

        // then
        assertThat(thrown).isInstanceOf(InvalidLlmNameException.class);
        InvalidLlmNameException exception = (InvalidLlmNameException) thrown;
        assertThat(exception.getCode()).isEqualTo(AiErrorCode.INVALID_LLM_NAME.getCode());
    }

    @Test
    @DisplayName("activate should set isActive to true")
    void activate_shouldSetIsActiveToTrue() {
        // given
        Llm llm = Llm.create("gemini-1.5-flash", false);

        // when
        llm.activate();

        // then
        assertThat(llm.isActive()).isTrue();
    }

    @Test
    @DisplayName("deactivate should set isActive to false")
    void deactivate_shouldSetIsActiveToFalse() {
        // given
        Llm llm = Llm.create("gemini-1.5-flash", true);

        // when
        llm.deactivate();

        // then
        assertThat(llm.isActive()).isFalse();
    }

    @Test
    @DisplayName("softDelete should mark entity as deleted")
    void softDelete_shouldMarkEntityAsDeleted() {
        // given
        Llm llm = Llm.create("gemini-1.5-flash", false);
        Long deletedBy = 1L;

        // when
        llm.softDelete(deletedBy);

        // then
        assertThat(llm.isDeleted()).isTrue();
        assertThat(llm.getDeletedBy()).isEqualTo(deletedBy);
        assertThat(llm.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("softDelete should keep first deleted state when called twice")
    void softDelete_shouldKeepFirstDeletedState_whenCalledTwice() {
        // given
        Llm llm = Llm.create("gemini-1.5-flash", false);

        // when
        llm.softDelete(1L);
        LocalDateTime firstDeletedAt = llm.getDeletedAt();
        Long firstDeletedBy = llm.getDeletedBy();

        llm.softDelete(2L);

        // then
        assertThat(llm.isDeleted()).isTrue();
        assertThat(llm.getDeletedAt()).isEqualTo(firstDeletedAt);
        assertThat(llm.getDeletedBy()).isEqualTo(firstDeletedBy);
    }
}