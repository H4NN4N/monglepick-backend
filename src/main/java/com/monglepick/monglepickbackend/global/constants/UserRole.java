package com.monglepick.monglepickbackend.global.constants;

/**
 * 사용자 역할 열거형.
 *
 * <p>사용자의 권한 수준을 정의한다.
 * Spring Security 권한 접두사 "ROLE_"과 조합하여 사용된다.</p>
 *
 * @see com.monglepick.monglepickbackend.domain.user.entity.User
 */
public enum UserRole {

    /** 일반 사용자 */
    USER,

    /** 관리자 */
    ADMIN
}
