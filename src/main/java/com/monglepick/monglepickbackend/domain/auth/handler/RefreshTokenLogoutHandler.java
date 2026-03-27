package com.monglepick.monglepickbackend.domain.auth.handler;

import com.monglepick.monglepickbackend.domain.auth.service.JwtService;
import com.monglepick.monglepickbackend.global.security.CookieUtil;
import com.monglepick.monglepickbackend.global.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.util.StringUtils;

/**
 * 로그아웃 시 Refresh Token을 DB 화이트리스트에서 삭제하는 핸들러.
 *
 * <p>KMG 프로젝트의 RefreshTokenLogoutHandler 패턴을 적용.
 * 쿠키 보안 수정: 기존의 요청 Body JSON 파싱(getInputStream) 코드를 제거하고
 * CookieUtil.extractRefreshToken()으로 교체하였다.
 * Refresh Token은 HttpOnly 쿠키로만 전달되므로 body에서 읽는 것은 불필요하다.</p>
 *
 * <p>로그아웃 처리 흐름:</p>
 * <ol>
 *   <li>쿠키에서 Refresh Token 추출</li>
 *   <li>토큰 유효성 검증 후 DB 화이트리스트에서 삭제 (무효화)</li>
 *   <li>클라이언트 쿠키 삭제 (Set-Cookie MaxAge=0)</li>
 * </ol>
 */
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenLogoutHandler implements LogoutHandler {

    private final JwtService jwtService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Refresh Token 쿠키 추출/삭제를 담당하는 단일 유틸리티.
     * body 파싱 없이 쿠키에서 직접 Refresh Token을 읽는다.
     */
    private final CookieUtil cookieUtil;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response,
                        Authentication authentication) {

        /* 쿠키에서 Refresh Token 추출 (body 파싱 코드 전면 제거) */
        String refreshToken = cookieUtil.extractRefreshToken(request);

        /* 쿠키가 없거나 빈 값이면 이미 로그아웃된 상태 — 조용히 종료 */
        if (!StringUtils.hasText(refreshToken)) {
            log.debug("로그아웃 요청 — refreshToken 쿠키 없음 (이미 로그아웃 상태)");
            return;
        }

        /* Refresh Token 유효성 검증 후 DB 화이트리스트에서 삭제 */
        if (jwtTokenProvider.validateToken(refreshToken)) {
            jwtService.removeRefresh(refreshToken);
            log.info("로그아웃 — Refresh Token DB에서 삭제 완료");
        } else {
            log.warn("로그아웃 — 유효하지 않은 Refresh Token (이미 만료되었거나 변조됨)");
        }

        /* 클라이언트 쿠키 삭제 (Set-Cookie MaxAge=0 전송) */
        cookieUtil.deleteRefreshTokenCookie(response);
    }
}
