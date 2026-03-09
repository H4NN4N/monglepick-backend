package com.monglepick.monglepickbackend.dto.response;

import com.monglepick.monglepickbackend.entity.Review;

import java.time.LocalDateTime;

/**
 * 리뷰 응답 DTO
 *
 * <p>리뷰 목록 및 상세 조회 시 반환되는 데이터입니다.</p>
 *
 * @param id 리뷰 ID
 * @param movieId 리뷰 대상 영화 ID
 * @param rating 평점 (0.5~5.0)
 * @param content 리뷰 본문
 * @param author 작성자 닉네임
 * @param createdAt 작성 시각
 */
public record ReviewResponse(
        Long id,
        Long movieId,
        Double rating,
        String content,
        String author,
        LocalDateTime createdAt
) {
    /**
     * Review 엔티티로부터 ReviewResponse를 생성하는 팩토리 메서드
     *
     * @param review Review 엔티티 (user가 페치되어 있어야 함)
     * @return ReviewResponse 인스턴스
     */
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getMovieId(),
                review.getRating(),
                review.getContent(),
                review.getUser().getNickname(),
                review.getCreatedAt()
        );
    }
}
