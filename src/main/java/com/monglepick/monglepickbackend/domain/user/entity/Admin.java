package com.monglepick.monglepickbackend.domain.user.entity;

import com.monglepick.monglepickbackend.global.entity.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 관리자 엔티티 — admin 테이블 매핑.
 *
 * <p>관리자 권한을 가진 사용자의 부가 정보를 저장한다.
 * users 테이블과 1:1 관계이며, user_id로 연결된다.
 * 각 사용자당 하나의 관리자 레코드만 존재한다 (user_id UNIQUE).</p>
 *
 * <h3>주요 필드</h3>
 * <ul>
 *   <li>{@code userId} — 사용자 ID (FK → users.user_id, UNIQUE)</li>
 *   <li>{@code adminRole} — 관리자 역할 (기본값: "ADMIN")</li>
 *   <li>{@code isActive} — 관리자 활성 여부</li>
 *   <li>{@code lastLoginAt} — 마지막 로그인 시각</li>
 * </ul>
 */
@Entity
@Table(
        name = "admin",
        uniqueConstraints = @UniqueConstraint(columnNames = "user_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
/**
 * BaseTimeEntity → BaseAuditEntity로 변경: created_by, updated_by 감사 필드 추가 상속
 */
public class Admin extends BaseAuditEntity {

    /** 관리자 레코드 고유 ID (BIGINT AUTO_INCREMENT PK) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "admin_id")
    private Long adminId;

    /**
     * 사용자 ID (VARCHAR(50), NOT NULL, UNIQUE).
     * users.user_id를 참조한다.
     */
    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    /**
     * 관리자 역할 (최대 50자).
     * 기본값: "ADMIN".
     * 예: "ADMIN"(일반 관리자), "SUPER_ADMIN"(최고 관리자)
     */
    @Column(name = "admin_role", length = 50)
    @Builder.Default
    private String adminRole = "ADMIN";

    /**
     * 관리자 활성 여부.
     * 기본값: true.
     * false로 설정하면 관리자 권한이 비활성화된다.
     */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * 마지막 로그인 시각.
     * 관리자가 마지막으로 로그인한 시점을 기록한다.
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
}
