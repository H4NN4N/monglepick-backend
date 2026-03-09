package com.monglepick.monglepickbackend.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * 리뷰 작성 요청 DTO
 *
 * <p>POST /api/v1/reviews 요청 본문에 사용됩니다.</p>
 *
 * @param movieId 리뷰 대상 영화 ID (필수)
 * @param rating 평점 (필수, 0.5~5.0)
 * @param content 리뷰 본문 (선택)
 */
public record ReviewCreateRequest(
        @NotNull(message = "영화 ID는 필수입니다.")
        Long movieId,

        @NotNull(message = "평점은 필수입니다.")
        @DecimalMin(value = "0.5", message = "평점은 0.5 이상이어야 합니다.")
        @DecimalMax(value = "5.0", message = "평점은 5.0 이하여야 합니다.")
        Double rating,

        String content
) {
}
