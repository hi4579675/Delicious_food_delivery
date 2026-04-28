package com.sparta.delivery.ai.domain.repository;

import com.sparta.delivery.ai.domain.entity.Llm;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface LlmRepository extends JpaRepository<Llm, UUID> {

    Optional<Llm> findByLlmId(UUID llmId);

    Page<Llm> findByLlmNameContainingIgnoreCase(String keyword, Pageable pageable);

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Llm l WHERE l.isActive = true AND l.deletedAt IS NULL")
    Optional<Llm> findByIsActiveTrueForUpdate();
}