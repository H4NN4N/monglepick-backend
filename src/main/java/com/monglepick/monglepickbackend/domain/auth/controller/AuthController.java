package com.monglepick.monglepickbackend.domain.auth.controller;

import com.monglepick.monglepickbackend.domain.auth.dto.AuthDto.AuthResponse;
import com.monglepick.monglepickbackend.domain.auth.dto.AuthDto.LoginRequest;
import com.monglepick.monglepickbackend.domain.auth.dto.AuthDto.OAuthRequest;
import com.monglepick.monglepickbackend.domain.auth.dto.AuthDto.RefreshRequest;
import com.monglepick.monglepickbackend.domain.auth.dto.AuthDto.SignupRequest;
import com.monglepick.monglepickbackend.domain.auth.dto.AuthDto.TokenResponse;
import com.monglepick.monglepickbackend.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 컨트롤러 — 회원가입, 로그인, 소셜 로그인, 토큰 갱신 REST API 엔드포인트.
 *
 * <p>모든 인증 관련 엔드포인트는 {@code /api/v1/auth/**} 경로 하위에 위치하며,
 * Spring Security에서 {@code permitAll()}로 설정되어 인증 없이 접근 가능하다.</p>
 *
 * <h3>엔드포인트 목록</h3>
 * <table>
 *   <tr><th>메서드</th><th>경로</th><th>설명</th></tr>
 *   <tr><td>POST</td><td>/api/v1/auth/signup</td><td>로컬 회원가입</td></tr>
 *   <tr><td>POST</td><td>/api/v1/auth/login</td><td>로컬 로그인</td></tr>
 *   <tr><td>POST</td><td>/api/v1/auth/oauth/{provider}</td><td>소셜 로그인 (google/kakao/naver)</td></tr>
 *   <tr><td>POST</td><td>/api/v1/auth/refresh</td><td>토큰 갱신</td></tr>
 *   <tr><td>POST</td><td>/api/v1/auth/logout</td><td>로그아웃 (stateless, 200 반환)</td></tr>
 * </table>
 *
 * <h3>응답 형식</h3>
 * <p>DTO를 직접 반환한다 (ApiResponse 래퍼 미사용).
 * {@link com.monglepick.monglepickbackend.domain.reward.controller.PointController}와 동일한 패턴이다.</p>
 *
 * @see AuthService
 */
@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {

    /** 인증 서비스 (회원가입/로그인/OAuth/토큰갱신 비즈니스 로직) */
    private final AuthService authService;

    /**
     * 로컬 회원가입.
     *
     * <p>이메일+비밀번호 기반의 자체 회원가입을 처리한다.
     * 성공 시 201 Created와 함께 JWT 토큰 쌍 + 사용자 정보를 반환한다.</p>
     *
     * <h4>요청 예시</h4>
     * <pre>{@code
     * POST /api/v1/auth/signup
     * Content-Type: application/json
     *
     * {
     *   "email": "user@example.com",
     *   "password": "mypassword123",
     *   "nickname": "몽글유저"
     * }
     * }</pre>
     *
     * <h4>성공 응답 (201 Created)</h4>
     * <pre>{@code
     * {
     *   "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
     *   "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
     *   "user": {
     *     "id": "uuid-...",
     *     "email": "user@example.com",
     *     "nickname": "몽글유저",
     *     "profileImage": null,
     *     "provider": "LOCAL",
     *     "role": "USER"
     *   }
     * }
     * }</pre>
     *
     * <h4>에러 응답</h4>
     * <ul>
     *   <li>409 — 이메일 중복: {@code {"code": "A001", "message": "이미 사용 중인 이메일입니다"}}</li>
     *   <li>409 — 닉네임 중복: {@code {"code": "A002", "message": "이미 사용 중인 닉네임입니다"}}</li>
     * </ul>
     *
     * @param request 회원가입 요청 (이메일, 비밀번호, 닉네임)
     * @return 201 Created + AuthResponse (토큰 쌍 + 사용자 정보)
     */
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        log.info("POST /api/v1/auth/signup — email: {}", request.email());

        AuthResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 로컬 로그인.
     *
     * <p>이메일+비밀번호 기반의 자체 로그인을 처리한다.
     * 성공 시 JWT 토큰 쌍 + 사용자 정보를 반환한다.</p>
     *
     * <h4>요청 예시</h4>
     * <pre>{@code
     * POST /api/v1/auth/login
     * Content-Type: application/json
     *
     * {
     *   "email": "user@example.com",
     *   "password": "mypassword123"
     * }
     * }</pre>
     *
     * <h4>성공 응답 (200 OK)</h4>
     * <pre>{@code
     * {
     *   "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
     *   "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
     *   "user": { "id": "uuid-...", "email": "user@example.com", ... }
     * }
     * }</pre>
     *
     * <h4>에러 응답</h4>
     * <ul>
     *   <li>401 — 자격증명 불일치: {@code {"code": "A003", "message": "이메일 또는 비밀번호가 올바르지 않습니다"}}</li>
     *   <li>409 — 소셜 가입 이메일: {@code {"code": "A007", "message": "해당 이메일로 이미 다른 방식으로 가입되어 있습니다"}}</li>
     * </ul>
     *
     * @param request 로그인 요청 (이메일, 비밀번호)
     * @return 200 OK + AuthResponse (토큰 쌍 + 사용자 정보)
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /api/v1/auth/login — email: {}", request.email());

        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 소셜 로그인 (OAuth).
     *
     * <p>Google, Kakao, Naver 소셜 로그인을 처리한다.
     * 클라이언트가 소셜 제공자로부터 받은 인가 코드를 전달하면,
     * 백엔드에서 토큰 교환 → 사용자 정보 조회 → JWT 발급을 수행한다.</p>
     *
     * <h4>요청 예시</h4>
     * <pre>{@code
     * POST /api/v1/auth/oauth/google
     * Content-Type: application/json
     *
     * {
     *   "code": "4/0AXE...",
     *   "redirectUri": "http://localhost:5173/oauth/callback"
     * }
     * }</pre>
     *
     * <h4>성공 응답 (200 OK)</h4>
     * <pre>{@code
     * {
     *   "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
     *   "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
     *   "user": { "id": "uuid-...", "provider": "GOOGLE", ... }
     * }
     * }</pre>
     *
     * <h4>에러 응답</h4>
     * <ul>
     *   <li>401 — OAuth 실패: {@code {"code": "A006", "message": "소셜 로그인에 실패했습니다"}}</li>
     *   <li>409 — 이메일 충돌: {@code {"code": "A007", "message": "해당 이메일로 이미 다른 방식으로 가입되어 있습니다"}}</li>
     * </ul>
     *
     * @param provider 소셜 제공자 이름 (google, kakao, naver)
     * @param request  소셜 로그인 요청 (인가 코드, 리다이렉트 URI)
     * @return 200 OK + AuthResponse (토큰 쌍 + 사용자 정보)
     */
    @PostMapping("/oauth/{provider}")
    public ResponseEntity<AuthResponse> oauthLogin(
            @PathVariable String provider,
            @Valid @RequestBody OAuthRequest request) {
        log.info("POST /api/v1/auth/oauth/{} — code: {}...", provider,
                request.code().length() > 10 ? request.code().substring(0, 10) : request.code());

        AuthResponse response = authService.oauthLogin(provider, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 토큰 갱신.
     *
     * <p>Refresh Token을 사용하여 새로운 Access Token + Refresh Token 쌍을 발급받는다.
     * Access Token이 만료되었을 때 클라이언트가 호출한다.</p>
     *
     * <h4>요청 예시</h4>
     * <pre>{@code
     * POST /api/v1/auth/refresh
     * Content-Type: application/json
     *
     * {
     *   "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
     * }
     * }</pre>
     *
     * <h4>성공 응답 (200 OK)</h4>
     * <pre>{@code
     * {
     *   "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
     *   "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
     * }
     * }</pre>
     *
     * <h4>에러 응답</h4>
     * <ul>
     *   <li>401 — 유효하지 않은 토큰: {@code {"code": "A004", "message": "유효하지 않은 토큰입니다"}}</li>
     * </ul>
     *
     * @param request 토큰 갱신 요청 (리프레시 토큰)
     * @return 200 OK + TokenResponse (새 토큰 쌍)
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(@Valid @RequestBody RefreshRequest request) {
        log.info("POST /api/v1/auth/refresh");

        TokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 로그아웃.
     *
     * <p>JWT 기반 stateless 인증이므로 서버 측 상태 변경이 없다.
     * 클라이언트가 토큰을 폐기(삭제)하면 로그아웃 처리된다.
     * 이 엔드포인트는 클라이언트의 로그아웃 플로우 호환성을 위해 200 OK를 반환한다.</p>
     *
     * @return 200 OK (빈 응답 본문)
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        log.info("POST /api/v1/auth/logout — stateless 로그아웃 처리");

        // JWT 기반 stateless 인증이므로 서버 측 별도 처리 불필요
        // 클라이언트에서 저장된 토큰을 삭제하면 로그아웃 완료
        return ResponseEntity.ok().build();
    }
}
