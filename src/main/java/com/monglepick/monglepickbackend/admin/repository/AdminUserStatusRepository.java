package com.monglepick.monglepickbackend.admin.repository;

import com.monglepick.monglepickbackend.domain.user.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 관리자 전용 사용자 제재 이력(UserStatus) 리포지토리.
 *
 * <p>user_status 테이블은 계정 정지/복구 이력을 INSERT-ONLY 원장으로 기록한다.
 * UserStatus 엔티티는 김민규 user 도메인이지만, admin 관리는 윤형주 admin 도메인에서
 * 별도 JpaRepository로 처리한다 (AdminReportRepository와 동일 패턴).</p>
 *
 * <h3>주요 쿼리</h3>
 * <ul>
 *   <li>{@link #findByUserIdOrderByCreatedAtDesc} — 특정 사용자의 제재 이력 전체 (최신순)</li>
 * </ul>
 */
public interface AdminUserStatusRepository extends JpaRepository<UserStatus, Long> {

    /**
     * 특정 사용자의 제재 이력 전체 조회 (최신순).
     *
     * <p>정지(SUSPENDED) 및 복구(ACTIVE) 이력을 모두 포함한다.</p>
     *
     * @param userId 조회할 사용자 ID
     * @return 제재 이력 목록 (createdAt DESC)
     */
    List<UserStatus> findByUserIdOrderByCreatedAtDesc(String userId);
}
