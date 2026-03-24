package com.monglepick.monglepickbackend.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 인증 시스템 DTO 모음.
 *
 * <p>모든 인증 관련 요청/응답 DTO를 inner record로 정의한다.
 * {@link com.monglepick.monglepickbackend.domain.reward.dto.PointDto}와
 * 동일한 패턴을 따른다 (final 클래스 + private 생성자 + inner record).</p>
 *
 * <h3>요청 DTO</h3>
 * <ul>
 *   <li>{@link SignupRequest} — 로컬 회원가입 요청 (이메일, 비밀번호, 닉네임)</li>
 *   <li>{@link LoginRequest} — 로컬 로그인 요청 (이메일, 비밀번호)</li>
 *   <li>{@link OAuthRequest} — 소셜 로그인 요청 (인가 코드, 리다이렉트 URI)</li>
 *   <li>{@link RefreshRequest} — 토큰 갱신 요청 (리프레시 토큰)</li>
 * </ul>
 *
 * <h3>응답 DTO</h3>
 * <ul>
 *   <li>{@link AuthResponse} — 인증 성공 응답 (토큰 쌍 + 사용자 정보)</li>
 *   <li>{@link UserInfo} — 사용자 요약 정보 (AuthResponse 내부에 포함)</li>
 *   <li>{@link TokenResponse} — 토큰 갱신 응답 (새 토큰 쌍)</li>
 * </ul>
 *
 * @see com.monglepick.monglepickbackend.domain.auth.controller.AuthController
 * @see com.monglepick.monglepickbackend.domain.auth.service.AuthService
 */
public final class AuthDto {

    /** 인스턴스 생성 방지 (유틸리티 클래스 패턴) */
    private AuthDto() {
    }

    // ──────────────────────────────────────────────
    // 로컬 회원가입
    // ──────────────────────────────────────────────

    /**
     * 로컬 회원가입 요청.
     *
     * <p>이메일+비밀번호 기반의 자체 회원가입 시 사용된다.
     * 모든 필드에 유효성 검증 어노테이션이 적용되어 있다.</p>
     *
     * @param email    이메일 주소 (필수, 이메일 형식)
     * @param password 비밀번호 (필수, 8~128자)
     * @param nickname 닉네임 (필수, 2~20자)
     */
    public record SignupRequest(
            @NotBlank(message = "이메일은 필수입니다")
            @Email(message = "올바른 이메일 형식이 아닙니다")
            String email,

            @NotBlank(message = "비밀번호는 필수입니다")
            @Size(min = 8, max = 128, message = "비밀번호는 8자 이상이어야 합니다")
            String password,

            @NotBlank(message = "닉네임은 필수입니다")
            @Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다")
            String nickname
    ) {
    }

    // ──────────────────────────────────────────────
    // 로컬 로그인
    // ──────────────────────────────────────────────

    /**
     * 로컬 로그인 요청.
     *
     * <p>이메일+비밀번호 기반의 자체 로그인 시 사용된다.</p>
     *
     * @param email    이메일 주소 (필수, 이메일 형식)
     * @param password 비밀번호 (필수)
     */
    public record LoginRequest(
            @NotBlank(message = "이메일은 필수입니다")
            @Email(message = "올바른 이메일 형식이 아닙니다")
            String email,

            @NotBlank(message = "비밀번호는 필수입니다")
            String password
    ) {
    }

    // ──────────────────────────────────────────────
    // 소셜 로그인 (OAuth)
    // ──────────────────────────────────────────────

    /**
     * 소셜 로그인(OAuth) 요청.
     *
     * <p>클라이언트가 소셜 로그인 인가 코드를 받은 후,
     * 백엔드에 토큰 교환을 요청할 때 사용된다.</p>
     *
     * @param code        인가 코드 (필수, 소셜 제공자로부터 받은 코드)
     * @param redirectUri 리다이렉트 URI (선택, 토큰 교환 시 필요)
     */
    public record OAuthRequest(
            @NotBlank(message = "인가 코드는 필수입니다")
            String code,

            String redirectUri
    ) {
    }

    // ──────────────────────────────────────────────
    // 토큰 갱신
    // ──────────────────────────────────────────────

    /**
     * 토큰 갱신 요청.
     *
     * <p>Access Token이 만료되었을 때, Refresh Token으로
     * 새로운 Access Token + Refresh Token 쌍을 발급받는다.</p>
     *
     * @param refreshToken 리프레시 토큰 (필수)
     */
    public record RefreshRequest(
            @NotBlank(message = "리프레시 토큰은 필수입니다")
            String refreshToken
    ) {
    }

    // ──────────────────────────────────────────────
    // 인증 성공 응답
    // ──────────────────────────────────────────────

    /**
     * 인증 성공 응답 (로그인/회원가입/소셜 로그인 공통).
     *
     * <p>Access Token + Refresh Token 쌍과 사용자 기본 정보를 포함한다.
     * 클라이언트는 이 응답을 받아 토큰을 저장하고, 이후 API 호출 시 사용한다.</p>
     *
     * @param accessToken  JWT Access Token (Authorization 헤더에 사용)
     * @param refreshToken JWT Refresh Token (토큰 갱신 시 사용)
     * @param user         사용자 요약 정보
     */
    public record AuthResponse(
            String accessToken,
            String refreshToken,
            UserInfo user
    ) {
    }

    /**
     * 사용자 요약 정보.
     *
     * <p>인증 응답에 포함되는 사용자 기본 정보이다.
     * 클라이언트가 로그인 직후 사용자 정보를 표시하는 데 사용된다.</p>
     *
     * @param id           사용자 ID (PK)
     * @param email        이메일 주소
     * @param nickname     닉네임
     * @param profileImage 프로필 이미지 URL (nullable)
     * @param provider     로그인 제공자 (LOCAL, GOOGLE, KAKAO, NAVER)
     * @param role         사용자 역할 (USER, ADMIN)
     */
    public record UserInfo(
            String id,
            String email,
            String nickname,
            String profileImage,
            String provider,
            String role
    ) {
    }

    // ──────────────────────────────────────────────
    // 토큰 갱신 응답
    // ──────────────────────────────────────────────

    /**
     * 토큰 갱신 응답.
     *
     * <p>Refresh Token으로 새로운 토큰 쌍을 발급받은 결과이다.</p>
     *
     * @param accessToken  새로운 JWT Access Token
     * @param refreshToken 새로운 JWT Refresh Token
     */
    public record TokenResponse(
            String accessToken,
            String refreshToken
    ) {
    }
}
