package com.monglepick.monglepickbackend.domain.auth.handler;

import com.monglepick.monglepickbackend.domain.auth.service.JwtService;
import com.monglepick.monglepickbackend.domain.user.entity.User;
import com.monglepick.monglepickbackend.domain.user.repository.UserRepository;
import com.monglepick.monglepickbackend.global.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 로컬 로그인 성공 핸들러.
 *
 * <p>KMG 프로젝트의 LoginSuccessHandler 패턴을 적용.
 * 로그인 성공 시 Access Token + Refresh Token을 생성하고,
 * Refresh Token을 DB 화이트리스트에 저장한 후 JSON 응답으로 반환한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException {

        /* principal은 AuthService.loadUserByUsername()에서 반환한 username(= email) */
        String email = authentication.getName();

        /* 이메일로 User 엔티티 조회 → userId로 JWT 생성 */
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("로그인 성공 후 사용자 조회 실패: " + email));

        /* Access Token + Refresh Token 생성 */
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUserId(), user.getUserRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId());

        /* Refresh Token을 DB 화이트리스트에 저장 */
        jwtService.addRefresh(user.getUserId(), refreshToken);

        /* JSON 응답 반환 */
        response.setContentType("application/json;charset=UTF-8");
        String json = String.format(
                "{\"accessToken\":\"%s\",\"refreshToken\":\"%s\",\"user\":{\"id\":\"%s\",\"email\":\"%s\",\"nickname\":\"%s\",\"profileImage\":%s,\"provider\":\"%s\",\"role\":\"%s\"}}",
                accessToken, refreshToken,
                user.getUserId(), user.getEmail(), user.getNickname(),
                user.getProfileImage() != null ? "\"" + user.getProfileImage() + "\"" : "null",
                user.getProvider().name(), user.getUserRole()
        );
        response.getWriter().write(json);
        response.getWriter().flush();

        log.info("로컬 로그인 성공 — userId: {}, email: {}", user.getUserId(), email);
    }
}
