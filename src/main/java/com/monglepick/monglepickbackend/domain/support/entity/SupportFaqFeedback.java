package com.monglepick.monglepickbackend.domain.support.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * FAQ 피드백 엔티티.
 *
 * <p>MySQL {@code support_faq_feedback} 테이블과 매핑된다.
 * 사용자가 FAQ에 대해 "도움됨(helpful=true)" 또는 "도움 안됨(helpful=false)" 피드백을 제출한다.</p>
 *
 * <h3>중복 방지</h3>
 * <p>(faq_id, user_id) 복합 유니크 제약으로 동일 사용자가 같은 FAQ에 중복 피드백을 제출할 수 없다.
 * DB 레벨 제약 위반 시 {@code DataIntegrityViolationException}이 발생하며,
 * 서비스 레이어에서 사전 중복 확인 후 {@code ErrorCode.FAQ_FEEDBACK_DUPLICATE} 예외로 변환한다.</p>
 *
 * <h3>BaseAuditEntity 미사용 이유</h3>
 * <p>이 엔티티는 단순 피드백 기록 용도이므로 {@code created_at}만 필요하다.
 * {@code updated_at}, {@code created_by}, {@code updated_by}는 불필요하므로
 * {@link BaseAuditEntity} 대신 {@code @CreationTimestamp}만 직접 선언한다.</p>
 *
 * <h3>카운터 반정규화 연동</h3>
 * <p>피드백 저장 시 서비스 레이어에서 {@link SupportFaq#incrementHelpful()} 또는
 * {@link SupportFaq#incrementNotHelpful()}을 함께 호출하여 FAQ의 집계 카운터를 동기화한다.</p>
 */
@Entity
@Table(
        name = "support_faq_feedback",
        uniqueConstraints = {
                // 동일 사용자가 같은 FAQ에 중복 피드백 제출 방지
                @UniqueConstraint(
                        name = "uk_faq_feedback_faq_user",
                        columnNames = {"faq_id", "user_id"}
                )
        },
        indexes = {
                // FAQ별 피드백 조회 시 사용
                @Index(name = "idx_faq_feedback_faq", columnList = "faq_id"),
                // 사용자별 피드백 이력 조회 시 사용
                @Index(name = "idx_faq_feedback_user", columnList = "user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupportFaqFeedback {

    /**
     * 피드백 고유 식별자 (BIGINT AUTO_INCREMENT PK).
     * DB가 자동 생성하며 변경 불가.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Long feedbackId;

    /**
     * 피드백 대상 FAQ.
     * LAZY 로딩 — 피드백 조회 시 FAQ 전체 내용이 항상 필요하지 않으므로 지연 로딩을 적용한다.
     * nullable = false: 반드시 유효한 FAQ에 연결되어야 한다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faq_id", nullable = false)
    private SupportFaq faq;

    /**
     * 피드백 제출 사용자 ID (VARCHAR 50).
     * User 엔티티를 직접 참조하지 않고 userId 문자열로 보관한다.
     * (피드백은 단순 집계 목적이므로 User JOIN이 불필요)
     */
    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    /**
     * 피드백 유형.
     * true  — "도움됨" (SupportFaq.helpfulCount 증가)
     * false — "도움 안됨" (SupportFaq.notHelpfulCount 증가)
     */
    @Column(nullable = false)
    private boolean helpful;

    /**
     * 피드백 제출 시각.
     * INSERT 시 Hibernate가 자동으로 현재 시각을 설정한다.
     * updatable = false: 생성 이후 변경 불가.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 생성자 (빌더 패턴).
     *
     * @param faq     피드백 대상 FAQ 엔티티
     * @param userId  피드백 제출 사용자 ID
     * @param helpful true: 도움됨, false: 도움 안됨
     */
    @Builder
    public SupportFaqFeedback(SupportFaq faq, String userId, boolean helpful) {
        this.faq = faq;
        this.userId = userId;
        this.helpful = helpful;
    }
}
