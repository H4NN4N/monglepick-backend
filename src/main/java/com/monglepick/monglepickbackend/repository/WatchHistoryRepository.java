package com.monglepick.monglepickbackend.repository;

import com.monglepick.monglepickbackend.entity.WatchHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 시청 이력 JPA 리포지토리
 *
 * <p>MySQL watch_history 테이블에 대한 데이터 접근 레이어입니다.
 * 26M+ 행의 대용량 테이블이므로 반드시 페이징을 사용해야 합니다.</p>
 */
@Repository
public interface WatchHistoryRepository extends JpaRepository<WatchHistory, Long> {

    /**
     * 특정 사용자의 시청 이력을 페이징으로 조회합니다.
     * <p>마이페이지에서 시청 이력을 표시할 때 사용됩니다.
     * 최신순 정렬은 Pageable에서 지정합니다.</p>
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 및 정렬 정보
     * @return 페이지 단위의 시청 이력
     */
    Page<WatchHistory> findByUserId(Long userId, Pageable pageable);
}
