package com.monglepick.monglepickbackend.domain.auth.handler;

import com.monglepick.monglepickbackend.domain.auth.service.JwtService;
import com.monglepick.monglepickbackend.global.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * OAuth2 소셜 로그인 성공 핸들러.
 *
 * <p>KMG 프로젝트의 SocialSuccessHandler 패턴을 적용.
 * 소셜 로그인 성공 시 Refresh Token만 생성하여 HttpOnly 쿠키에 저장하고,
 * 클라이언트의 /cookie 페이지로 리다이렉트한다.
 * 클라이언트는 이후 /jwt/exchange를 호출하여 쿠키를 헤더 기반 JWT로 교환한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SocialSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtService jwtService;

    /** 프론트엔드 리다이렉트 URL (환경변수로 오버라이드 가능) */
    @Value("${app.oauth.redirect-url:http://localhost:5173/cookie}")
    private String redirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException {

        /* principal은 CustomOAuth2User.getName() = userId */
        String userId = authentication.getName();

        /* Refresh Token만 생성 (Access Token은 /jwt/exchange에서 발급) */
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId);

        /* Refresh Token을 DB 화이트리스트에 저장 */
        jwtService.addRefresh(userId, refreshToken);

        /* HttpOnly 쿠키에 Refresh Token 저장 (10초 TTL, 클라이언트가 즉시 교환해야 함) */
        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(false); // TODO: 운영 환경에서는 true로 변경
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(10);

        response.addCookie(refreshCookie);

        /* 클라이언트의 /cookie 페이지로 리다이렉트 */
        response.sendRedirect(redirectUrl);

        log.info("소셜 로그인 성공 — userId: {}, 리다이렉트: {}", userId, redirectUrl);
    }
}
