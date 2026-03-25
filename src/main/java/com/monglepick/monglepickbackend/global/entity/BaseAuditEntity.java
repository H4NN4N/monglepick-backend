package com.monglepick.monglepickbackend.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 감사 로그 공통 부모 엔티티 (BaseTimeEntity 확장).
 *
 * <p>{@link BaseTimeEntity}의 {@code created_at}, {@code updated_at}에 더하여
 * {@code created_by}(등록자), {@code updated_by}(수정자)를 자동 관리한다.</p>
 *
 * <ul>
 *   <li>{@code created_by} — 레코드 최초 생성자 ID (INSERT 시 자동 설정, 이후 변경 불가)</li>
 *   <li>{@code updated_by} — 레코드 최종 수정자 ID (UPDATE 시 자동 갱신)</li>
 * </ul>
 *
 * <p>사용자 ID는 {@link AuditorAwareImpl}에서 SecurityContext를 통해 자동 주입된다.</p>
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseAuditEntity extends BaseTimeEntity {

    /**
     * 레코드 생성자 ID.
     * INSERT 시 AuditorAware를 통해 현재 사용자 ID가 자동 설정되며,
     * 이후 UPDATE 시에는 변경되지 않는다.
     */
    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 50)
    private String createdBy;

    /**
     * 레코드 최종 수정자 ID.
     * UPDATE 시 AuditorAware를 통해 현재 사용자 ID로 자동 갱신된다.
     */
    @LastModifiedBy
    @Column(name = "updated_by", length = 50)
    private String updatedBy;
}
