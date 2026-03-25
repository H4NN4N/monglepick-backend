package com.monglepick.monglepickbackend.domain.auth.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

/**
 * Spring Security OAuth2User 커스텀 구현체.
 *
 * <p>소셜 로그인 성공 후 SecurityContext에 저장되는 인증 객체이다.
 * getName()이 userId를 반환하도록 오버라이드하여
 * 이후 핸들러에서 userId 기반으로 JWT를 발급한다.</p>
 */
@Getter
@RequiredArgsConstructor
public class CustomOAuth2User implements OAuth2User {

    /** OAuth2 제공자로부터 받은 사용자 속성 맵 */
    private final Map<String, Object> attributes;

    /** Spring Security 권한 목록 (ROLE_USER) */
    private final Collection<? extends GrantedAuthority> authorities;

    /** 사용자 ID (users.user_id) — principal로 사용 */
    private final String userId;

    /** 사용자 이메일 (로그 및 닉네임 fallback용) */
    private final String userEmail;

    /**
     * Spring Security에서 principal로 사용하는 값.
     * userId를 반환하여 JWT subject와 일관성을 유지한다.
     */
    @Override
    public String getName() {
        return userId;
    }
}
