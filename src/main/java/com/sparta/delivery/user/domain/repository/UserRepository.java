package com.sparta.delivery.user.domain.repository;

import com.sparta.delivery.user.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(Long id);

    /**
     * 로그인 시 사용. Soft Delete된 계정은 제외하고 조회.
     */
    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    /**
     * 회원가입 시 중복 체크용.
     */
    boolean existsByEmail(String email);

    /**
     * 관리자용 사용자 목록 조회. 삭제되지 않은 계정만.
     */
    Page<User> findAllByDeletedAtIsNull(Pageable pageable);
}