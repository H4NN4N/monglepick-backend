package com.monglepick.monglepickbackend.domain.recommendation.dto;

import com.monglepick.monglepickbackend.domain.recommendation.entity.RecommendationLog;
import com.monglepick.monglepickbackend.domain.review.entity.Review;

import java.time.LocalDateTime;

/**
 * 추천 이력 도메인 DTO 모음 (record 기반).
 *
 * <p>클라이언트 추천 이력 탭({@code GET /api/v1/recommendations})에서 사용하는
 * 요청·응답 DTO를 하나의 파일에 Inner Record로 묶어 관리한다.</p>
 *
 * <h3>찜/봤어요 상태 처리 전략</h3>
 * <p>RecommendationLog 엔티티에는 wishlist_yn, watched_yn 컬럼이 없다.
 * 대신 {@link com.monglepick.monglepickbackend.domain.recommendation.entity.RecommendationImpact}의
 * {@code wishlisted}, {@code watched} 필드를 활용하여 상태를 조회하고,
 * 토글 시에도 해당 Impact 레코드를 업서트한다.</p>
 *
 * <h3>포함된 DTO</h3>
 * <ul>
 *   <li>{@link RecommendationHistoryResponse} — 추천 이력 목록 항목 응답</li>
 *   <li>{@link WishlistToggleResponse} — 찜 토글 응답</li>
 *   <li>{@link WatchedToggleResponse} — 봤어요 토글 응답</li>
 * </ul>
 */
public class RecommendationHistoryDto {

    // ─────────────────────────────────────────────
    // 응답 DTO
    // ─────────────────────────────────────────────

    /**
     * 추천 이력 목록 항목 응답 DTO.
     *
     * <p>{@code GET /api/v1/recommendations} 페이징 응답의 content 항목으로 사용된다.
     * RecommendationLog 엔티티와 연관 Impact 상태를 조합하여 생성한다.</p>
     *
     * <h3>응답 예시 (JSON)</h3>
     * <pre>{@code
     * {
     *   "recommendationLogId": 42,
     *   "movieId": "tmdb_12345",
     *   "title": "인터스텔라",
     *   "posterPath": "/path/to/poster.jpg",
     *   "genres": "[\"SF\",\"드라마\"]",
     *   "score": 0.92,
     *   "reason": "우주 탐험과 감동적인 부자 관계를 좋아하시는 분께 추천합니다.",
     *   "recommendedAt": "2026-04-06T10:30:00",
     *   "wishlisted": false,
     *   "watched": false
     * }
     * }</pre>
     *
     * @param recommendationLogId 추천 로그 고유 ID
     * @param movieId             추천된 영화 ID
     * @param title               영화 한국어 제목
     * @param posterPath          TMDB 포스터 이미지 경로 (null 가능)
     * @param genres              장르 목록 JSON 문자열 (null 가능)
     * @param score               최종 추천 점수
     * @param reason              AI가 생성한 추천 이유 텍스트
     * @param recommendedAt       추천 발생 시각 (RecommendationLog.createdAt)
     * @param wishlisted          찜 여부 (RecommendationImpact.wishlisted 기반)
     * @param watched             봤어요 여부 (RecommendationImpact.watched 기반)
     */
    public record RecommendationHistoryResponse(

            /** 추천 로그 고유 ID — 찜/봤어요 토글 API의 경로 파라미터로 사용 */
            Long recommendationLogId,

            /** 추천된 영화 ID */
            String movieId,

            /** 영화 한국어 제목 */
            String title,

            /** TMDB 포스터 이미지 경로 (없으면 null) */
            String posterPath,

            /** 장르 목록 JSON 문자열 (예: ["SF","드라마"], 없으면 null) */
            String genres,

            /** 최종 추천 점수 (0.0~1.0 범위) */
            Float score,

            /** AI가 생성한 추천 이유 텍스트 */
            String reason,

            /** 추천 발생 시각 */
            LocalDateTime recommendedAt,

            /** 찜 여부 (RecommendationImpact.wishlisted, 임팩트 없으면 false) */
            boolean wishlisted,

            /** 봤어요 여부 (RecommendationImpact.watched, 임팩트 없으면 false) */
            boolean watched,

            /**
             * 별점 (1~5 정수, 없으면 null).
             *
             * <p>2026-04-27 통합: reviews 테이블의 활성 리뷰({@code is_deleted=false})에서
             * (user_id, movie_id) 매칭으로 가져온다. {@code reviews.rating} 은 Double(0.5 단위)
             * 이지만 추천 카드 UI 는 정수 1~5점 표시이므로 반올림하여 Integer 로 노출한다.
             * 기존 {@code recommendation_feedback.rating} 경로(Integer 1~5)와 시맨틱 호환.</p>
             */
            Integer feedbackRating,

            /**
             * 리뷰 본문 (없으면 null).
             *
             * <p>2026-04-27 통합: reviews 테이블의 {@code contents} 컬럼.
             * 기존 {@code recommendation_feedback.comment} 경로 대체.</p>
             */
            String feedbackComment

    ) {
        /**
         * RecommendationLog + Impact 상태 + 활성 Review 를 조합해 응답 DTO 를 생성한다.
         *
         * <p>2026-04-27 재정의: 기존 {@code RecommendationFeedback} 인자를 폐기하고
         * {@link Review} 로 교체. 추천 카드의 별점·코멘트는 reviews 단일 진실 원본에서
         * 복원되며, review 가 null 이면 두 feedback* 필드 모두 null 로 채운다.</p>
         *
         * @param log        추천 로그 엔티티 (movie JOIN FETCH 필수)
         * @param wishlisted 찜 여부 (Impact 없으면 false)
         * @param watched    봤어요 여부 (Impact 없으면 false)
         * @param review     해당 영화의 활성 리뷰 (없으면 null)
         * @return 추천 이력 응답 DTO
         */
        public static RecommendationHistoryResponse from(
                RecommendationLog log,
                boolean wishlisted,
                boolean watched,
                Review review
        ) {
            // reviews.rating(Double, 0.5 단위) → 추천 카드 UI 의 Integer 1~5 로 반올림 변환
            Integer ratingInt = (review != null && review.getRating() != null)
                    ? (int) Math.round(review.getRating())
                    : null;

            return new RecommendationHistoryResponse(
                    log.getRecommendationLogId(),
                    log.getMovie().getMovieId(),
                    log.getMovie().getTitle(),
                    log.getMovie().getPosterPath(),
                    log.getMovie().getGenres(),
                    log.getScore(),
                    log.getReason(),
                    log.getCreatedAt(),     // BaseAuditEntity.createdAt → 추천 발생 시각
                    wishlisted,
                    watched,
                    ratingInt,
                    review != null ? review.getContent() : null
            );
        }
    }

    /**
     * 찜 토글 응답 DTO.
     *
     * <p>{@code POST /api/v1/recommendations/{recommendationLogId}/wishlist} 응답에 사용된다.
     * 토글 후 현재 찜 상태를 반환한다.</p>
     *
     * @param wishlisted 토글 후 찜 여부 (true: 찜 추가됨, false: 찜 취소됨)
     */
    public record WishlistToggleResponse(

            /** 토글 후 찜 여부 */
            boolean wishlisted

    ) {}

    /**
     * 봤어요 토글 응답 DTO.
     *
     * <p>{@code POST /api/v1/recommendations/{recommendationLogId}/watched} 응답에 사용된다.
     * 토글 후 현재 봤어요 상태를 반환한다.</p>
     *
     * @param watched 토글 후 봤어요 여부 (true: 봤어요 추가됨, false: 봤어요 취소됨)
     */
    public record WatchedToggleResponse(

            /** 토글 후 봤어요 여부 */
            boolean watched

    ) {}

    /**
     * 관심없음 토글 응답 DTO (P2, 2026-04-24).
     *
     * <p>{@code POST /api/v1/recommendations/{recommendationLogId}/dismiss} 응답에 사용된다.
     * 토글 후 현재 dismissed 상태를 반환한다. true 인 경우 Chat Agent 의 다음 추천에서
     * 해당 영화가 exclude_ids 에 자동 포함되어 재추천되지 않는다.</p>
     *
     * @param dismissed 토글 후 관심없음 여부 (true: 표시됨 → 다음 추천 제외, false: 취소됨)
     */
    public record DismissedToggleResponse(

            /** 토글 후 관심없음 여부 */
            boolean dismissed

    ) {}
}
