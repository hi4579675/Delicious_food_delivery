package com.sparta.delivery.user.domain.repository;

import com.sparta.delivery.user.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /** 로그인 시 사용 */
    Optional<User> findByEmail(String email);

    /** 회원가입 중복 체크 */
    boolean existsByEmail(String email);


}