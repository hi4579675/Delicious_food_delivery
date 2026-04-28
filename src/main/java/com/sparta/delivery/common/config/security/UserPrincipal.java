package com.sparta.delivery.common.config.security;

public interface UserPrincipal {

    /** 사용자 PK (p_user.user_id) */
    Long getId();

    /** 로그인 아이디 */
    String getUsername();

    /** 권한: CUSTOMER | OWNER | MANAGER | MASTER */
    String getRole();
}