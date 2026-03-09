package com.monglepick.monglepickbackend.repository;

import com.monglepick.monglepickbackend.entity.RecommendationFeedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 추천 피드백 JPA 리포지토리
 *
 * <p>MySQL recommendation_feedback 테이블에 대한 데이터 접근 레이어입니다.
 * AI 추천 결과에 대한 사용자 피드백을 관리합니다.</p>
 */
@Repository
public interface RecommendationFeedbackRepository extends JpaRepository<RecommendationFeedback, Long> {

    /**
     * 특정 사용자의 추천 피드백 목록을 페이징으로 조회합니다.
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 페이지 단위의 피드백 목록
     */
    Page<RecommendationFeedback> findByUserId(Long userId, Pageable pageable);
}
