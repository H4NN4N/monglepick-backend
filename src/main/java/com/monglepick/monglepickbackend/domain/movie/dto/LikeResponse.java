package com.monglepick.monglepickbackend.domain.movie.dto;

/**
 * 영화 좋아요 응답 DTO.
 *
 * <p>toggleLike, isLiked 등 좋아요 관련 API의 공통 응답 형식이다.
 * 클라이언트는 이 DTO 하나로 현재 좋아요 상태와 전체 좋아요 수를 한 번에 받는다.</p>
 *
 * <h3>⚠️ 2026-04-07 DEPRECATED — monglepick-recommend(FastAPI)로 이관</h3>
 * <p>recommend FastAPI의 {@code app/model/schema.py LikeResponse}(Pydantic 모델)가
 * 동일한 JSON 필드(`liked`, `likeCount`)를 반환하므로, Frontend는 이관 전후로
 * 동일한 응답 구조를 받는다. 본 record는 fallback 경로에서만 사용된다.</p>
 *
 * <h3>사용 시나리오</h3>
 * <ul>
 *   <li>POST /api/v1/movies/{movieId}/like — 토글 후 변경된 상태 반환</li>
 *   <li>GET  /api/v1/movies/{movieId}/like — 현재 사용자의 좋아요 상태 조회</li>
 *   <li>GET  /api/v1/movies/{movieId}/like/count — 공개 좋아요 수 조회 (liked=false 고정)</li>
 * </ul>
 *
 * @param liked     현재 사용자의 활성 좋아요 여부 (true=좋아요 활성, false=취소 또는 미로그인)
 * @param likeCount 해당 영화의 전체 활성 좋아요 수
 * @deprecated 2026-04-07 — movie Like 도메인 이관. recommend FastAPI의 LikeResponse 사용.
 *     상세: docs/movie_like_recommend_migration.md
 */
@Deprecated(since = "2026-04-07", forRemoval = false)
public record LikeResponse(
        boolean liked,
        long likeCount
) {

    /**
     * 좋아요 상태와 카운트로 LikeResponse를 생성하는 팩토리 메서드.
     *
     * @param liked     현재 사용자의 좋아요 활성 여부
     * @param likeCount 해당 영화의 전체 활성 좋아요 수
     * @return LikeResponse 인스턴스
     */
    public static LikeResponse of(boolean liked, long likeCount) {
        return new LikeResponse(liked, likeCount);
    }
}
