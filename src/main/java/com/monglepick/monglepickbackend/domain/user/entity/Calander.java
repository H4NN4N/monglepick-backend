package com.monglepick.monglepickbackend.domain.user.entity;

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

import java.time.LocalDateTime;

/**
 * 사용자 캘린더 엔티티 — calander 테이블 매핑.
 *
 * <p>사용자의 영화 관련 일정(시사회, 개봉일 알림, 개인 메모 등)을 저장한다.
 * 시작 시간은 필수이며, 종료 시간은 선택이다.</p>
 *
 * <h3>테이블명 참고</h3>
 * <p>DDL(init.sql v4_t2)에서 테이블명이 "calander"로 정의되어 있어
 * 오타(calendar)이지만 DDL과의 호환성을 위해 그대로 유지한다.</p>
 *
 * <h3>주요 필드</h3>
 * <ul>
 *   <li>{@code userId} — 사용자 ID</li>
 *   <li>{@code scheduleTitle} — 일정 제목 (필수)</li>
 *   <li>{@code scheduleDescription} — 일정 설명 (선택)</li>
 *   <li>{@code startTime} — 시작 시간 (DATETIME, 필수)</li>
 *   <li>{@code endTime} — 종료 시간 (DATETIME, 선택)</li>
 * </ul>
 */
@Entity
@Table(name = "calander")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
/**
 * BaseTimeEntity → BaseAuditEntity로 변경: created_by, updated_by 감사 필드 추가 상속
 */
public class Calander extends BaseAuditEntity {

    /** 캘린더 일정 고유 ID (BIGINT AUTO_INCREMENT PK) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "calander_id")
    private Long calanderId;

    /**
     * 사용자 ID (VARCHAR(50), NOT NULL).
     * users.user_id를 참조한다.
     */
    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    /** 일정 제목 (필수, 최대 200자) */
    @Column(name = "schedule_title", length = 200, nullable = false)
    private String scheduleTitle;

    /** 일정 설명 (TEXT 타입, 선택) */
    @Column(name = "schedule_description", columnDefinition = "TEXT")
    private String scheduleDescription;

    /**
     * 일정 시작 시간 (DATETIME, NOT NULL).
     * LocalDateTime 타입으로 매핑된다.
     */
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    /**
     * 일정 종료 시간 (DATETIME, nullable).
     * 종일 일정이나 시작 시간만 있는 경우 null.
     */
    @Column(name = "end_time")
    private LocalDateTime endTime;
}
