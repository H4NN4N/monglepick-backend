package com.monglepick.monglepickbackend.domain.recommendation.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * 추천 카드 리뷰 작성/갱신 요청 DTO (2026-04-27 신설).
 *
 * <p>{@code POST /api/v1/recommendations/{recommendationLogId}/review} 의 요청 바디로 사용된다.
 * 기존 {@code RecommendationFeedbackRequest} 가 폐기되면서 추천 카드의 별점/코멘트는
 * reviews 테이블로 저장 경로가 일원화됐다. 같은 영화에 활성 리뷰가 있으면 업데이트,
 * 없으면 신규 작성된다 (UPSERT).</p>
 *
 * <p>movieId 와 reviewSource/reviewCategoryCode 는 서버 측에서 recommendation_log 매핑으로
 * 자동 결정되므로 클라이언트는 별점·본문만 보내면 된다.</p>
 *
 * @param rating  별점 (0.5 ~ 5.0, 0.5 단위 권장)
 * @param content 리뷰 본문 (선택, null/빈 문자열 허용)
 */
public record RecommendationReviewRequest(

        @NotNull(message = "별점은 필수입니다.")
        @DecimalMin(value = "0.5", message = "별점은 0.5 이상이어야 합니다.")
        @DecimalMax(value = "5.0", message = "별점은 5.0 이하여야 합니다.")
        Double rating,

        String content

) {}
