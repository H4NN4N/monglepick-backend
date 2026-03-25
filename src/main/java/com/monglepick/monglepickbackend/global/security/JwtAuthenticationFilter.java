package com.monglepick.monglepickbackend.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 인증 필터 — 매 요청마다 Authorization 헤더의 Bearer 토큰을 검증한다.
 *
 * <p>Spring Security 필터 체인에서 {@link ServiceKeyAuthFilter} 뒤,
 * {@code UsernamePasswordAuthenticationFilter} 앞에 배치되어
 * 클라이언트(브라우저)의 JWT 인증을 처리한다.</p>
 *
 * <h3>인증 흐름</h3>
 * <ol>
 *   <li>Authorization 헤더에서 "Bearer " 접두사를 가진 토큰 추출</li>
 *   <li>Bearer 헤더가 없거나 토큰이 유효하지 않으면 → 다음 필터로 위임 (거부하지 않음)</li>
 *   <li>토큰이 유효하고 Refresh Token이 아니면 → SecurityContext에 인증 정보 설정</li>
 *   <li>Refresh Token이면 → 인증을 설정하지 않고 다음 필터로 위임
 *       (Refresh Token은 /api/v1/auth/refresh 엔드포인트에서만 직접 처리)</li>
 * </ol>
 *
 * <h3>스킵 경로</h3>
 * <ul>
 *   <li>{@code /health} — 헬스체크</li>
 *   <li>{@code /api/v1/auth/**} — 인증 관련 API (로그인, 회원가입 등)</li>
 * </ul>
 *
 * @see JwtTokenProvider
 * @see ServiceKeyAuthFilter
 * @see SecurityConfig
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Authorization 헤더에서 토큰을 추출할 때 사용하는 접두사 */
    private static final String BEARER_PREFIX = "Bearer ";

    /** JWT 토큰 생성/검증을 담당하는 프로바이더 */
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 매 요청마다 실행되는 JWT 인증 필터 로직.
     *
     * <p>Authorization 헤더에서 Bearer 토큰을 추출하여 유효성을 검증하고,
     * 유효한 경우 {@link SecurityContextHolder}에 인증 정보를 설정한다.</p>
     *
     * @param request     HTTP 요청 객체
     * @param response    HTTP 응답 객체
     * @param filterChain 다음 필터로 요청을 전달하기 위한 체인
     * @throws ServletException 서블릿 처리 중 예외
     * @throws IOException      I/O 처리 중 예외
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // ── 1단계: Authorization 헤더에서 Bearer 토큰 추출 ──
        String token = extractToken(request);

        // ── 2단계: 토큰이 없으면 인증 없이 다음 필터로 위임 ──
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // ── 3단계: 토큰 1회 파싱 (검증 + 클레임 추출 동시 수행) ──
        JwtTokenProvider.ParsedToken parsed = jwtTokenProvider.parse(token);

        if (parsed == null) {
            // 유효하지 않은 토큰 → 인증을 설정하지 않고 다음 필터로 위임
            log.debug("유효하지 않은 JWT 토큰 — URI: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        // ── 4단계: Refresh Token은 인증 컨텍스트에 설정하지 않음 ──
        // Refresh Token은 /api/v1/auth/refresh 엔드포인트에서만 직접 사용
        if (parsed.isRefresh()) {
            log.debug("Refresh Token은 인증 컨텍스트에 설정하지 않음 — URI: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        // ── 5단계: 유효한 Access Token → SecurityContext에 인증 정보 설정 ──
        String userId = parsed.userId();
        String role = parsed.role();

        // role이 null이면 기본값 "USER" 사용
        String authority = "ROLE_" + (role != null ? role : "USER");

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,                                              // principal: 사용자 ID
                        null,                                                // credentials: 불필요
                        List.of(new SimpleGrantedAuthority(authority))        // 권한 목록
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("JWT 인증 성공 — userId: {}, role: {}", userId, role);

        filterChain.doFilter(request, response);
    }

    /**
     * JWT 인증을 스킵할 경로인지 판별한다.
     *
     * <p>헬스체크, 인증 관련 API, OAuth2 흐름 경로, JWT 토큰 교환/갱신은
     * JWT 검증이 불필요하다.</p>
     *
     * @param request HTTP 요청 객체
     * @return 스킵 대상이면 true
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/health")
                || path.startsWith("/api/v1/auth/")
                || path.startsWith("/oauth2/")
                || path.startsWith("/login/oauth2/")
                || path.startsWith("/login")
                || path.startsWith("/jwt/");
    }

    /**
     * HTTP 요청의 Authorization 헤더에서 Bearer 토큰을 추출한다.
     *
     * @param request HTTP 요청 객체
     * @return Bearer 토큰 문자열 (헤더가 없거나 형식이 다르면 null)
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
