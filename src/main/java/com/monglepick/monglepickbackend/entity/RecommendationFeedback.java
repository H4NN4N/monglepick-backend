package com.monglepick.monglepickbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 추천 피드백 엔티티
 *
 * <p>MySQL recommendation_feedback 테이블과 매핑됩니다.
 * AI가 추천한 영화에 대한 사용자의 피드백(좋아요/싫어요/관심없음)을 저장합니다.</p>
 *
 * <p>이 피드백 데이터는 추천 정확도 개선에 활용됩니다:</p>
 * <ul>
 *   <li>LIKE: 추천이 적절했음 → 유사 패턴 강화</li>
 *   <li>DISLIKE: 추천이 부적절했음 → 해당 패턴 약화</li>
 *   <li>NOT_INTERESTED: 관심 없는 영화 → 향후 추천에서 제외</li>
 * </ul>
 */
@Entity
@Table(name = "recommendation_feedback")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationFeedback {

    /** 피드백 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 피드백 제공 사용자 (지연 로딩) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 피드백 대상 영화 ID */
    @Column(name = "movie_id", nullable = false)
    private Long movieId;

    /** 피드백 유형 (LIKE, DISLIKE, NOT_INTERESTED) */
    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_type", nullable = false, length = 20)
    private FeedbackType feedbackType;

    /** 피드백 제출 시각 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 피드백 유형 열거형
     */
    public enum FeedbackType {
        /** 추천이 마음에 들었음 */
        LIKE,
        /** 추천이 마음에 들지 않았음 */
        DISLIKE,
        /** 해당 영화에 관심 없음 */
        NOT_INTERESTED
    }

    @Builder
    public RecommendationFeedback(User user, Long movieId, FeedbackType feedbackType) {
        this.user = user;
        this.movieId = movieId;
        this.feedbackType = feedbackType;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
