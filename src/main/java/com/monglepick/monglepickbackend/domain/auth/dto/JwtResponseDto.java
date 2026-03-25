package com.monglepick.monglepickbackend.domain.auth.dto;

/**
 * JWT 토큰 응답 DTO.
 *
 * <p>로컬 로그인 성공, 토큰 갱신, OAuth2 쿠키→헤더 교환 시 반환된다.</p>
 *
 * @param accessToken  JWT Access Token (1시간)
 * @param refreshToken JWT Refresh Token (7일)
 * @param userNickname 사용자 닉네임 (프론트엔드 표시용)
 */
public record JwtResponseDto(
        String accessToken,
        String refreshToken,
        String userNickname
) {
}
