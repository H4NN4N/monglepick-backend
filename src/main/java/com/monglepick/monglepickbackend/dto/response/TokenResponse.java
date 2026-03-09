package com.monglepick.monglepickbackend.dto.response;

/**
 * 토큰 응답 DTO
 *
 * <p>로그인 및 토큰 갱신 시 반환되는 JWT 토큰 쌍입니다.</p>
 *
 * @param accessToken JWT 액세스 토큰 (API 인증용, 기본 1시간 유효)
 * @param refreshToken JWT 리프레시 토큰 (액세스 토큰 갱신용, 기본 7일 유효)
 */
public record TokenResponse(
        String accessToken,
        String refreshToken
) {
}
