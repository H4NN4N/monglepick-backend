package com.monglepick.monglepickbackend.domain.recommendation.controller;

import com.monglepick.monglepickbackend.domain.recommendation.dto.RecommendationReviewRequest;
import com.monglepick.monglepickbackend.domain.review.dto.ReviewResponse;
import com.monglepick.monglepickbackend.domain.review.service.ReviewService;
import com.monglepick.monglepickbackend.global.controller.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * 추천 카드 리뷰 컨트롤러 (2026-04-27 신설) — 추천 내역에서 작성한 별점/코멘트를
 * reviews 테이블로 직접 저장하는 통합 엔드포인트.
 *
 * <p>"봤다 = 리뷰" 단일 진실 원본 원칙(CLAUDE.md)에 따라, 기존
 * {@code RecommendationFeedbackController} 의 {@code POST /feedback} 경로를 폐기하고
 * 본 컨트롤러의 {@code POST /review} 경로로 일원화한다. 별점·코멘트가 reviews 테이블에
 * 저장됨으로써 CF 학습 단일 진실 원본에 즉시 흡수되며, user_watch_history /
 * recommendation_impact.rated / 리뷰 작성 리워드까지 자동 처리된다
 * ({@link ReviewService#createOrUpdateFromRecommendation} 위임).</p>
 *
 * <h3>엔드포인트</h3>
 * <ul>
 *   <li>POST /api/v1/recommendations/{recommendationLogId}/review — 별점/코멘트 UPSERT</li>
 * </ul>
 *
 * <h3>인증</h3>
 * <p>JWT Bearer 토큰 필수. 추천 로그 소유권은 {@code RecommendationLogRepository}
 * 가 (logId, userId) 조합으로 검증한다 (404 응답).</p>
 */
@Tag(name = "추천 카드 리뷰",
        description = "AI 추천 카드에서 작성한 별점/코멘트를 reviews 테이블로 UPSERT 저장")
@RestController
@RequestMapping("/api/v1/recommendations")
@Slf4j
@RequiredArgsConstructor
public class RecommendationReviewController extends BaseController {

    /** 리뷰 서비스 — UPSERT 비즈니스 로직 위임 */
    private final ReviewService reviewService;

    /**
     * 추천 카드에 별점·코멘트를 제출한다 (UPSERT).
     *
     * <p>같은 사용자가 같은 영화에 대해 활성 리뷰를 가지고 있으면 update,
     * 없으면 신규 작성된다. movie_id 는 recommendation_log JOIN 으로 자동 매핑.
     * reviewSource = {@code "rec_log_{logId}"}, reviewCategoryCode = {@code AI_RECOMMEND}
     * 으로 강제 세팅되어 출처가 추적 가능하다.</p>
     *
     * @param recommendationLogId 평가 대상 추천 로그 ID
     * @param request             별점·코멘트 요청 바디 ({@link RecommendationReviewRequest})
     * @param principal           JWT 인증 정보
     * @return 저장된 리뷰 응답 DTO
     */
    @Operation(
            summary = "추천 카드 별점/코멘트 제출 (UPSERT)",
            description = "추천 카드에서 별점과 코멘트를 reviews 테이블로 저장합니다. " +
                    "같은 영화에 활성 리뷰가 있으면 업데이트, 없으면 신규 작성됩니다. " +
                    "신규 작성 시 자동으로 user_watch_history 동기화 + " +
                    "recommendation_impact.rated 마킹 + 리뷰 작성 리워드 지급이 처리됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "저장 성공"),
            @ApiResponse(responseCode = "400", description = "별점이 0.5~5.0 범위를 벗어남"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "추천 이력이 없거나 본인 이력 아님")
    })
    @PostMapping("/{recommendationLogId}/review")
    public ResponseEntity<ReviewResponse> submitReview(
            @Parameter(description = "평가 대상 추천 로그 ID", required = true, example = "42")
            @PathVariable Long recommendationLogId,

            @RequestBody @Valid RecommendationReviewRequest request,

            Principal principal
    ) {
        String userId = resolveUserId(principal);
        log.info("추천 카드 리뷰 제출 요청: userId={}, recommendationLogId={}, rating={}",
                userId, recommendationLogId, request.rating());

        ReviewResponse response = reviewService.createOrUpdateFromRecommendation(
                userId,
                recommendationLogId,
                request.rating(),
                request.content()
        );

        return ResponseEntity.ok(response);
    }
}
