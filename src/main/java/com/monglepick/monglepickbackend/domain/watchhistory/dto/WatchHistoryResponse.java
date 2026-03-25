package com.monglepick.monglepickbackend.domain.watchhistory.dto;

import com.monglepick.monglepickbackend.domain.watchhistory.entity.WatchHistory;

import java.time.LocalDateTime;

/**
 * 시청 이력 응답 DTO.
 *
 * <p>WatchHistory 엔티티 대신 API 응답에 사용하여
 * 엔티티 내부 구조 노출과 Jackson 직렬화 문제를 방지한다.</p>
 *
 * @param watchHistoryId 시청 이력 ID
 * @param movieId        영화 ID
 * @param rating         사용자 평점 (nullable)
 * @param watchedAt      시청 일시
 */
public record WatchHistoryResponse(
        Long watchHistoryId,
        String movieId,
        Double rating,
        LocalDateTime watchedAt
) {
    /** 엔티티 → DTO 변환 팩토리 메서드 */
    public static WatchHistoryResponse from(WatchHistory entity) {
        return new WatchHistoryResponse(
                entity.getWatchHistoryId(),
                entity.getMovieId(),
                entity.getRating(),
                entity.getWatchedAt()
        );
    }
}
