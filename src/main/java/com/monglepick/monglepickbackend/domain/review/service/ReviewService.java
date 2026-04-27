package com.monglepick.monglepickbackend.domain.review.service;

import com.monglepick.monglepickbackend.domain.recommendation.entity.RecommendationImpact;
import com.monglepick.monglepickbackend.domain.recommendation.entity.RecommendationLog;
import com.monglepick.monglepickbackend.domain.recommendation.repository.RecommendationImpactRepository;
import com.monglepick.monglepickbackend.domain.recommendation.repository.RecommendationLogRepository;
import com.monglepick.monglepickbackend.domain.review.dto.ReviewCreateRequest;
import com.monglepick.monglepickbackend.domain.review.dto.ReviewResponse;
import com.monglepick.monglepickbackend.domain.reward.dto.RewardResult;
import com.monglepick.monglepickbackend.domain.review.dto.ReviewUpdateRequest;
import com.monglepick.monglepickbackend.domain.review.entity.Review;
import com.monglepick.monglepickbackend.domain.review.entity.ReviewCategoryCode;
import com.monglepick.monglepickbackend.domain.review.entity.ReviewLike;
import com.monglepick.monglepickbackend.domain.review.mapper.ReviewMapper;
import com.monglepick.monglepickbackend.domain.reward.service.RewardService;
import com.monglepick.monglepickbackend.domain.userwatchhistory.service.UserWatchHistoryService;
import com.monglepick.monglepickbackend.global.dto.LikeToggleResponse;
import com.monglepick.monglepickbackend.global.exception.BusinessException;
import com.monglepick.monglepickbackend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 리뷰 서비스
 *
 * <p>영화 리뷰의 CRUD + 좋아요 토글 비즈니스 로직을 처리한다.
 * JPA/MyBatis 하이브리드 §15에 따라 모든 데이터 접근은 {@link ReviewMapper}를 통해 이루어진다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    /** 리뷰/좋아요/투표 통합 Mapper */
    private final ReviewMapper reviewMapper;

    /** 리워드 서비스 */
    private final RewardService rewardService;

    /** 추천 임팩트 리포지토리 — 리뷰 작성 시 rated 플래그 업데이트 (윤형주 recommendation 도메인 유지) */
    private final RecommendationImpactRepository recommendationImpactRepository;

    /**
     * 추천 로그 리포지토리 — 추천 카드에서 리뷰 작성 시 movie_id/소유권 검증용
     * (recommendation_feedback 폐기 후 통합 경로, 2026-04-27).
     */
    private final RecommendationLogRepository recommendationLogRepository;

    /**
     * 시청 이력 서비스 — 리뷰 작성 시 user_watch_history 자동 동기화 (P0-2, 2026-04-24).
     *
     * <p>"봤다=리뷰" 부분 재정의 원칙(2026-04-08) 하에서 reviews(강한 신호) 와
     * user_watch_history(약한 신호) 가 별개 테이블로 운영되지만, 유저가 마이페이지를 거치지 않고
     * 바로 리뷰만 작성한 경우 user_watch_history 에 기록이 없어 마이페이지 시청 이력 UI 와
     * 정합성이 깨지는 문제가 있었다. 본 서비스의 {@code ensureWatchHistoryExists} 를
     * REQUIRES_NEW 트랜잭션으로 호출하여 정합성을 보장한다.</p>
     */
    private final UserWatchHistoryService userWatchHistoryService;

    /**
     * 영화 리뷰를 작성한다. 같은 사용자가 같은 영화에 중복 리뷰를 작성할 수 없다.
     */
    @Transactional
    public ReviewResponse createReview(String movieId, ReviewCreateRequest request, String userId) {
        // 1. 중복 리뷰 검사
        if (reviewMapper.existsByUserIdAndMovieId(userId, movieId)) {
            log.warn("리뷰 작성 실패 - 중복 리뷰: userId={}, movieId={}", userId, movieId);
            throw new BusinessException(ErrorCode.DUPLICATE_REVIEW);
        }

        // 2. 사용자 존재 검증은 JWT 인증 단계에서 처리됨 (§15.4)

        // 3. 리뷰 엔티티 생성 및 저장
        Review review = Review.builder()
                .userId(userId)
                .movieId(movieId)
                .rating(request.rating())
                .content(request.content())
                .reviewSource(request.reviewSource())
                .reviewCategoryCode(request.reviewCategoryCode())
                .build();

        // MyBatis insert — useGeneratedKeys로 reviewId 자동 세팅
        reviewMapper.insert(review);
        log.info("리뷰 작성 완료 - reviewId: {}, userId: {}, movieId: {}, reviewSource: {}, reviewCategoryCode: {}",
                review.getReviewId(), userId, movieId,
                request.reviewSource(), request.reviewCategoryCode());

        // user_watch_history 자동 동기화 (P0-2, 2026-04-24)
        // — "봤다 = 리뷰" 정합성 확보. 마이페이지를 거치지 않고 리뷰만 작성한 경우에도
        //   시청 이력 UI 에 즉시 반영되도록 보장.
        // — REQUIRES_NEW 별도 트랜잭션이므로 실패해도 본 트랜잭션은 영향 없음.
        //   추가 안전장치로 try/catch 까지 감싸 어떠한 watch_history 측 오류도
        //   리뷰 작성 흐름을 막지 않도록 한다.
        try {
            userWatchHistoryService.ensureWatchHistoryExists(userId, movieId, "review_context");
        } catch (Exception watchSyncErr) {
            log.warn("리뷰 작성 후 watch_history 동기화 실패 - reviewId:{}, userId:{}, movieId:{}, err:{}",
                    review.getReviewId(), userId, movieId, watchSyncErr.getMessage());
        }

        // 리워드 지급 — 결과를 캡처하여 응답에 포함
        int contentLength = request.content() != null ? request.content().length() : 0;
        RewardResult rewardResult = rewardService.grantReward(userId, "REVIEW_CREATE", "movie_" + movieId, contentLength);

        // 첫 리뷰 작성 보너스 — INSERT 후 카운트가 1이면 첫 리뷰
        long reviewCount = reviewMapper.countByUserId(userId);
        if (reviewCount == 1) {
            RewardResult firstResult = rewardService.grantReward(userId, "FIRST_REVIEW", "first_review_" + userId, 0);
            // 첫 리뷰 보너스가 지급되면 합산하여 응답에 포함
            if (firstResult.earned()) {
                rewardResult = RewardResult.of(
                        rewardResult.points() + firstResult.points(),
                        rewardResult.policyName()
                );
            }
        }

        // recommendation_impact.rated 업데이트 (퍼널 완성)
        // 윤형주 recommendation 도메인은 JPA 유지 — dirty checking 정상 동작
        List<RecommendationImpact> impacts =
                recommendationImpactRepository.findByUserIdAndMovieId(userId, movieId);
        if (!impacts.isEmpty()) {
            impacts.forEach(RecommendationImpact::markRated);
            log.debug("recommendation_impact.rated 업데이트 — userId:{}, movieId:{}, 건수:{}",
                    userId, movieId, impacts.size());
        }

        // 리워드 지급 포인트를 응답에 포함 (earned=true일 때만 포인트 표시)
        Integer rewardPoints = rewardResult.earned() ? rewardResult.points() : null;
        return ReviewResponse.from(review, rewardPoints);
    }

    /**
     * 추천 카드에서 별점/코멘트를 제출하면 reviews 테이블에 UPSERT 한다 (2026-04-27 신설).
     *
     * <p>"봤다 = 리뷰" 단일 진실 원본 원칙(CLAUDE.md)에 따라, 추천 내역 페이지의 별점 제출은
     * 더 이상 {@code recommendation_feedback} 으로 가지 않고 본 메서드를 통해 reviews 테이블에
     * 저장된다. 추천 카드는 같은 추천에 대해 별점을 여러 번 갱신할 수 있으므로
     * (user_id, movie_id) 활성 리뷰가 있으면 update, 없으면 create 로 동작한다.</p>
     *
     * <h3>처리 흐름</h3>
     * <ol>
     *   <li>recommendation_log 소유권 검증 (recommendationLogId × userId) — 없으면 REC001 (404)</li>
     *   <li>movie_id 추출 (JOIN FETCH 된 movie)</li>
     *   <li>활성 리뷰(is_deleted=false) 조회</li>
     *   <li>있으면 update — rating/contents 갱신, reward 미지급 (이미 부여 받은 리뷰)</li>
     *   <li>없으면 create — {@link #createReview(String, ReviewCreateRequest, String)} 위임 (reward + watch_history + impact.rated 자동 처리)</li>
     * </ol>
     *
     * <p>reviewSource = {@code "rec_log_{logId}"}, reviewCategoryCode = {@link ReviewCategoryCode#AI_RECOMMEND}
     * 으로 강제 세팅하여 리뷰의 출처가 "AI 추천 카드" 임을 명시한다.</p>
     *
     * @param userId              JWT 에서 추출한 사용자 ID
     * @param recommendationLogId 평가 대상 추천 로그 ID
     * @param rating              별점 (0.5 ~ 5.0)
     * @param content             리뷰 본문 (선택, null/빈 문자열 허용)
     * @return 저장된 리뷰 응답 DTO (신규 작성 시 reward 포인트 포함, update 시 null)
     * @throws BusinessException RECOMMENDATION_LOG_NOT_FOUND — 추천 로그가 없거나 본인 로그가 아닐 때
     */
    @Transactional
    public ReviewResponse createOrUpdateFromRecommendation(
            String userId,
            Long recommendationLogId,
            Double rating,
            String content) {

        // 1) 추천 로그 소유권 검증 + movie_id 획득 (movie JOIN FETCH 포함)
        RecommendationLog recLog = recommendationLogRepository
                .findByRecommendationLogIdAndUserId(recommendationLogId, userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RECOMMENDATION_LOG_NOT_FOUND,
                        "추천 이력을 찾을 수 없습니다: recommendationLogId=" + recommendationLogId));
        String movieId = recLog.getMovie().getMovieId();

        // 2) 활성 리뷰 단건 조회 — UPSERT 분기
        Review existing = reviewMapper.findByUserIdAndMovieId(userId, movieId);

        if (existing != null) {
            // 2-a) 기존 리뷰 update — content 는 null 허용 (별점만 갱신 케이스 지원)
            existing.update(rating, content);
            reviewMapper.update(existing);
            log.info("추천 카드 리뷰 update — reviewId:{}, userId:{}, movieId:{}, recLogId:{}",
                    existing.getReviewId(), userId, movieId, recommendationLogId);

            // recommendation_impact.rated 마킹 — 기존 리뷰가 있어도 funnel 지표 정합성 유지
            recommendationImpactRepository.findByUserIdAndMovieId(userId, movieId)
                    .forEach(RecommendationImpact::markRated);

            return ReviewResponse.from(existing);
        }

        // 2-b) 신규 리뷰 — createReview 에 위임 (reward + watch_history + impact.rated 일괄 처리)
        ReviewCreateRequest createRequest = new ReviewCreateRequest(
                movieId,
                rating,
                content,
                "rec_log_" + recommendationLogId,
                ReviewCategoryCode.AI_RECOMMEND
        );
        log.info("추천 카드 리뷰 신규 작성 위임 — userId:{}, movieId:{}, recLogId:{}",
                userId, movieId, recommendationLogId);
        return createReview(movieId, createRequest, userId);
    }

    /**
     * 특정 영화의 리뷰 목록을 페이징으로 조회한다 (닉네임 포함).
     */
    public Page<ReviewResponse> getReviewsByMovie(String movieId, Pageable pageable) {
        int offset = (int) pageable.getOffset();
        int limit  = pageable.getPageSize();

        List<Review> reviews = reviewMapper.findByMovieIdWithNickname(movieId, offset, limit);
        long total = reviewMapper.countByMovieId(movieId);

        List<ReviewResponse> content = reviews.stream().map(ReviewResponse::from).toList();
        return new PageImpl<>(content, pageable, total);
    }

    /**
     * 리뷰 좋아요 토글 (인스타그램 스타일).
     */
    @Transactional
    public LikeToggleResponse toggleReviewLike(String userId, Long reviewId) {
        ReviewLike existing = reviewMapper.findReviewLikeByReviewIdAndUserId(reviewId, userId);
        boolean liked;

        if (existing != null) {
            /* 좋아요 취소 — hard-delete */
            reviewMapper.deleteReviewLikeByReviewIdAndUserId(reviewId, userId);
            liked = false;
        } else {
            /* 좋아요 등록 — INSERT, race condition 처리 */
            try {
                reviewMapper.insertReviewLike(
                        ReviewLike.builder()
                                .reviewId(reviewId)
                                .userId(userId)
                                .build()
                );
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                log.warn("리뷰 좋아요 중복 INSERT 감지 (race condition) — userId:{}, reviewId:{}", userId, reviewId);
                reviewMapper.deleteReviewLikeByReviewIdAndUserId(reviewId, userId);
                long count = reviewMapper.countReviewLikeByReviewId(reviewId);
                return LikeToggleResponse.of(false, count);
            }
            liked = true;
        }

        long count = reviewMapper.countReviewLikeByReviewId(reviewId);
        log.debug("리뷰 좋아요 토글 — userId:{}, reviewId:{}, liked:{}, count:{}", userId, reviewId, liked, count);

        return LikeToggleResponse.of(liked, count);
    }

    /**
     * 리뷰 좋아요 수 조회 (비로그인 허용).
     */
    public long getReviewLikeCount(Long reviewId) {
        return reviewMapper.countReviewLikeByReviewId(reviewId);
    }

    /**
     * 리뷰 내용 및 평점을 수정한다. 작성자 본인만 수정 가능.
     */
    @Transactional
    public ReviewResponse updateReview(String movieId,
                                       Long reviewId,
                                       ReviewUpdateRequest request,
                                       String userId) {
        Review review = reviewMapper.findById(reviewId);
        if (review == null) {
            log.warn("리뷰 수정 실패 - 리뷰 없음: reviewId={}", reviewId);
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        // 경로 변수 movieId와 실제 리뷰의 movieId 일치 검증 (존재 정보 유출 방지를 위해 404)
        if (!review.getMovieId().equals(movieId)) {
            log.warn("리뷰 수정 실패 - 경로 movieId 불일치: path={}, review={}",
                    movieId, review.getMovieId());
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        // 작성자 본인 확인 (String FK 직접 비교)
        if (!review.getUserId().equals(userId)) {
            log.warn("리뷰 수정 실패 - 권한 없음: reviewId={}, 작성자={}, 요청자={}",
                    reviewId, review.getUserId(), userId);
            throw new BusinessException(ErrorCode.POST_ACCESS_DENIED);
        }

        // 도메인 메서드 + 명시 UPDATE (dirty checking 미지원)
        review.update(request.rating(), request.content());
        reviewMapper.update(review);

        log.info("리뷰 수정 완료 - reviewId: {}, userId: {}, movieId: {}", reviewId, userId, movieId);

        return ReviewResponse.from(review);
    }

    /**
     * 리뷰를 삭제한다. 작성자 본인만 삭제 가능.
     */
    @Transactional
    public void deleteReview(Long reviewId, String userId) {
        Review review = reviewMapper.findById(reviewId);
        if (review == null) {
            log.warn("리뷰 삭제 실패 - 리뷰 없음: reviewId={}", reviewId);
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        if (!review.getUserId().equals(userId)) {
            log.warn("리뷰 삭제 실패 - 권한 없음: reviewId={}, 작성자={}, 요청자={}",
                    reviewId, review.getUserId(), userId);
            throw new BusinessException(ErrorCode.POST_ACCESS_DENIED);
        }

        reviewMapper.deleteById(reviewId);
        log.info("리뷰 삭제 완료 - reviewId: {}, userId: {}", reviewId, userId);

        // 리워드 회수
        rewardService.revokeReward(userId, "REVIEW_CREATE", "movie_" + review.getMovieId());
    }
}
