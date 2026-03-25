package com.monglepick.monglepickbackend.domain.community.entity;

/* BaseAuditEntity 상속으로 created_at, updated_at, created_by, updated_by 자동 관리 */
import com.monglepick.monglepickbackend.global.entity.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게시글 신고 엔티티 — post_declaration 테이블 매핑.
 *
 * <p>사용자가 부적절한 게시글이나 댓글을 신고한 기록을 저장한다.
 * AI 독성 분석 점수(toxicity_score)와 처리 상태(status)를 관리한다.</p>
 *
 * <h3>주요 필드</h3>
 * <ul>
 *   <li>{@code postId} — 신고 대상 게시글 ID</li>
 *   <li>{@code categoryId} — 카테고리 ID (nullable)</li>
 *   <li>{@code userId} — 신고자 사용자 ID</li>
 *   <li>{@code reportedUserId} — 피신고자 (게시글/댓글 작성자) 사용자 ID</li>
 *   <li>{@code targetType} — 신고 대상 유형 (post, comment)</li>
 *   <li>{@code declarationContent} — 신고 사유 내용</li>
 *   <li>{@code toxicityScore} — AI 독성 분석 점수 (0.0~1.0, nullable)</li>
 *   <li>{@code status} — 처리 상태 (pending, reviewed, resolved, dismissed)</li>
 * </ul>
 *
 * <h3>변경 이력</h3>
 * <ul>
 *   <li>PK 필드명: declarationId → postDeclarationId (컬럼명: post_declaration_id)</li>
 *   <li>BaseAuditEntity 상속 추가 — created_at/updated_at/created_by/updated_by 자동 관리</li>
 *   <li>수동 createdAt 필드 및 @CreationTimestamp 제거 — BaseTimeEntity에서 상속</li>
 * </ul>
 */
@Entity
@Table(name = "post_declaration")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PostDeclaration extends BaseAuditEntity {

    /**
     * 신고 레코드 고유 ID (BIGINT AUTO_INCREMENT PK).
     * 필드명 변경: declarationId → postDeclarationId (컬럼명: post_declaration_id)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_declaration_id")
    private Long postDeclarationId;

    /**
     * 신고 대상 게시글 ID (BIGINT, NOT NULL).
     * posts.id를 참조한다.
     */
    @Column(name = "post_id", nullable = false)
    private Long postId;

    /**
     * 카테고리 ID (BIGINT, nullable).
     * category.category_id를 참조한다.
     */
    @Column(name = "category_id")
    private Long categoryId;

    /**
     * 신고자 사용자 ID (VARCHAR(50), NOT NULL).
     * 신고를 접수한 사용자를 식별한다.
     */
    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    /**
     * 피신고자 사용자 ID (VARCHAR(50), NOT NULL).
     * 신고 대상 게시글/댓글의 작성자를 식별한다.
     */
    @Column(name = "reported_user_id", length = 50, nullable = false)
    private String reportedUserId;

    /**
     * 신고 대상 유형 (최대 20자).
     * 기본값: "post".
     * "post": 게시글 신고, "comment": 댓글 신고.
     */
    @Column(name = "target_type", length = 20)
    @Builder.Default
    private String targetType = "post";

    /**
     * 신고 사유 내용 (TEXT 타입, NOT NULL).
     * 신고자가 작성한 신고 사유를 저장한다.
     */
    @Column(name = "declaration_content", columnDefinition = "TEXT", nullable = false)
    private String declarationContent;

    /**
     * AI 독성 분석 점수 (nullable).
     * 0.0 ~ 1.0 범위의 실수값. 1.0에 가까울수록 독성이 높다.
     * AI 분석이 수행되지 않은 경우 null.
     */
    @Column(name = "toxicity_score")
    private Float toxicityScore;

    /**
     * 처리 상태 (최대 20자).
     * 기본값: "pending".
     * "pending": 대기, "reviewed": 검토 중, "resolved": 처리 완료, "dismissed": 기각.
     */
    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "pending";

    /* created_at, updated_at → BaseTimeEntity에서 상속 */
    /* created_by, updated_by → BaseAuditEntity에서 상속 */
}
