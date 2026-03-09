package com.monglepick.monglepickbackend.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * AI 채팅 요청 DTO
 *
 * <p>POST /api/v1/chat 요청 본문에 사용됩니다.
 * FastAPI AI Agent로 프록시되어 영화 추천 대화를 수행합니다.</p>
 *
 * <p>이미지 필드는 base64 인코딩된 문자열로 전송됩니다.
 * VLM(Vision Language Model)이 이미지를 분석하여 영화를 추천합니다.</p>
 *
 * @param message 사용자 메시지 (필수)
 * @param image base64 인코딩된 이미지 (선택, 이미지 기반 추천 시 사용)
 * @param sessionId 세션 ID (선택, 멀티턴 대화 유지용, null이면 새 세션 생성)
 */
public record ChatRequest(
        @NotBlank(message = "메시지는 필수입니다.")
        String message,

        String image,

        String sessionId
) {
}
