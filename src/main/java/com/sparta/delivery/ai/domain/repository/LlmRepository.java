package com.sparta.delivery.ai.domain.repository;

import com.sparta.delivery.ai.domain.entity.Llm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface LlmRepository extends JpaRepository<Llm, UUID> {

    Optional<Llm> findByLlmId(UUID llmId);

    Optional<Llm> findByIsActiveTrue();

    @Query(
            value = """
                SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END
                FROM p_llms
                WHERE llm_name = :llmName
                """,
            nativeQuery = true
    )
    boolean existsIncludingDeletedByLlmName(@Param("llmName") String llmName);
}