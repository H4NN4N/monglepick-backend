package com.monglepick.monglepickbackend.domain.content.entity;

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
 * 유해성 검출 로그 엔티티 — toxicity_log 테이블 매핑.
 *
 * <p>AI Agent 또는 커뮤니티에서 감지된 유해/비속어 입력을 기록한다.
 * 사용자 입력 텍스트, 유해성 점수, 유해 유형, 취해진 조치를 저장하여
 * 콘텐츠 모더레이션 및 사후 분석에 활용된다.</p>
 *
 * <h3>주요 필드</h3>
 * <ul>
 *   <li>{@code userId} — 입력 사용자 ID (익명일 수 있으므로 nullable)</li>
 *   <li>{@code sessionId} — 채팅 세션 ID (채팅 외 컨텍스트에서는 nullable)</li>
 *   <li>{@code inputText} — 감지된 원본 입력 텍스트 (필수)</li>
 *   <li>{@code toxicityScore} — 유해성 점수 (0.0~1.0, 필수)</li>
 *   <li>{@code toxicityType} — 유해 유형 (예: "profanity", "hate_speech", "harassment")</li>
 *   <li>{@code actionTaken} — 취해진 조치 (기본값: "flagged")</li>
 * </ul>
 *
 * <h3>조치 유형 (actionTaken)</h3>
 * <ul>
 *   <li>{@code flagged} — 플래그 처리 (로그만 기록)</li>
 *   <li>{@code blocked} — 응답 차단</li>
 *   <li>{@code warned} — 경고 메시지 표시</li>
 *   <li>{@code filtered} — 유해 부분만 필터링</li>
 * </ul>
 *
 * <h3>변경 이력</h3>
 * <ul>
 *   <li>PK 필드명: id → toxicityLogId (컬럼명: toxicity_log_id)</li>
 *   <li>BaseAuditEntity 상속 추가 — created_at/updated_at/created_by/updated_by 자동 관리</li>
 *   <li>수동 createdAt 필드 및 @CreationTimestamp 제거 — BaseTimeEntity에서 상속</li>
 * </ul>
 */
@Entity
@Table(name = "toxicity_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ToxicityLog extends BaseAuditEntity {

    /**
     * 유해성 로그 고유 ID (BIGINT AUTO_INCREMENT PK).
     * 필드명 변경: id → toxicityLogId (엔티티 PK 네이밍 통일)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "toxicity_log_id")
    private Long toxicityLogId;

    /**
     * 입력 사용자 ID (VARCHAR(50)).
     * 익명 사용자이거나 시스템 감지인 경우 NULL일 수 있다.
     * FK 제약 없이 문자열로 저장한다 (DDL 기준).
     */
    @Column(name = "user_id", length = 50)
    private String userId;

    /**
     * 채팅 세션 ID (UUID v4).
     * 채팅 컨텍스트에서 감지된 경우에만 값이 있다.
     */
    @Column(name = "session_id", length = 36)
    private String sessionId;

    /** 감지된 원본 입력 텍스트 (필수) */
    @Column(name = "input_text", columnDefinition = "TEXT", nullable = false)
    private String inputText;

    /**
     * 유해성 점수 (0.0~1.0, 필수).
     * 0.0: 완전 안전, 1.0: 매우 유해.
     */
    @Column(name = "toxicity_score", nullable = false)
    private Float toxicityScore;

    /**
     * 유해 유형 (최대 50자).
     * 예: "profanity"(비속어), "hate_speech"(혐오발언),
     * "harassment"(괴롭힘), "sexual"(성적 콘텐츠)
     */
    @Column(name = "toxicity_type", length = 50)
    private String toxicityType;

    /**
     * 취해진 조치 (최대 50자).
     * 기본값: "flagged" (로그만 기록).
     * 예: "flagged", "blocked", "warned", "filtered"
     */
    @Column(name = "action_taken", length = 50)
    @Builder.Default
    private String actionTaken = "flagged";

    /* created_at, updated_at → BaseTimeEntity에서 상속 */
    /* created_by, updated_by → BaseAuditEntity에서 상속 */
}
