package com.monglepick.monglepickbackend.domain.roadmap.dto;

import com.monglepick.monglepickbackend.domain.roadmap.entity.CourseReview;

import java.time.LocalDateTime;

/**
 * 도장깨기 영화별 시청 리뷰 조회 응답 DTO.
 *
 * @param courseId       코스 슬러그
 * @param movieId        영화 ID
 * @param reviewText     사용자가 작성한 리뷰 본문 (작성하지 않았으면 null)
 * @param verified       시청 인증 완료 여부 (리뷰 레코드 존재 여부)
 * @param reviewStatus   인증 세부 상태 (PENDING/AUTO_VERIFIED/NEEDS_REVIEW/AUTO_REJECTED/ADMIN_APPROVED/ADMIN_REJECTED, null이면 미제출)
 * @param decisionReason 관리자/AI 판정 사유 (ADMIN_REJECTED일 때 사유 표시용, nullable)
 * @param createdAt      인증 시각
 */
public record CourseReviewResponse(
        String courseId,
        String movieId,
        String reviewText,
        boolean verified,
        String reviewStatus,
        String decisionReason,
        LocalDateTime createdAt
) {
    public static CourseReviewResponse from(CourseReview review, String reviewStatus, String decisionReason) {
        return new CourseReviewResponse(
                review.getCourseId(),
                review.getMovieId(),
                review.getReviewText(),
                true,
                reviewStatus,
                decisionReason,
                review.getCreatedAt()
        );
    }

    public static CourseReviewResponse notVerified(String courseId, String movieId) {
        return new CourseReviewResponse(courseId, movieId, null, false, null, null, null);
    }
}
