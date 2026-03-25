package com.monglepick.monglepickbackend.domain.auth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * OAuth2 소셜 로그인 실패 핸들러.
 *
 * <p>KMG 프로젝트의 SocialFailureHandler 패턴을 적용.
 * 소셜 로그인 실패 시 에러 메시지를 URL 파라미터로 인코딩하여
 * 클라이언트의 로그인 페이지로 리다이렉트한다.</p>
 */
@Slf4j
@Component
public class SocialFailureHandler implements AuthenticationFailureHandler {

    /** 프론트엔드 로그인 페이지 URL (환경변수로 오버라이드 가능) */
    @Value("${app.oauth.failure-url:http://localhost:5173/login}")
    private String failureUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                         AuthenticationException exception) throws IOException {

        String errorMessage = URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8);
        String redirectUrl = failureUrl + "?error=" + errorMessage;

        log.warn("소셜 로그인 실패 — error: {}", exception.getMessage());

        response.sendRedirect(redirectUrl);
    }
}
