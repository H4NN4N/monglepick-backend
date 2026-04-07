package com.monglepick.monglepickbackend.domain.movie.repository;

import com.monglepick.monglepickbackend.domain.movie.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 영화 좋아요 리포지토리.
 *
 * <p>likes 테이블에 대한 JPA 쿼리를 제공한다.
 * 소프트 삭제(deleted_at) 기반으로 활성/취소 좋아요를 구분한다.</p>
 *
 * <h3>⚠️ 2026-04-07 DEPRECATED — monglepick-recommend(FastAPI)로 이관</h3>
 * <p>영화 좋아요 도메인은 Redis 캐싱 + write-behind 패턴을 채택하면서
 * monglepick-recommend(FastAPI)로 이관되었다. 본 리포지토리는 Nginx 프록시가
 * 적용되지 않은 환경(로컬 개발 등)에서 fallback용으로만 유지된다.</p>
 * <p>신규 쿼리 메서드는 {@code monglepick-recommend/app/v2/repository/like_repository.py}의
 * {@code LikeRepository}(Raw SQL)에 추가해야 한다.</p>
 *
 * <h3>소프트 삭제 정책</h3>
 * <ul>
 *   <li>활성 좋아요: {@code deleted_at IS NULL}</li>
 *   <li>취소된 좋아요: {@code deleted_at IS NOT NULL}</li>
 * </ul>
 *
 * @see Like
 * @deprecated 2026-04-07 — movie Like 도메인 이관. 신규 수정 금지. 상세: docs/movie_like_recommend_migration.md
 */
@Deprecated(since = "2026-04-07", forRemoval = false)
public interface LikeRepository extends JpaRepository<Like, Long> {

    /**
     * 사용자 ID와 영화 ID로 좋아요 레코드를 조회한다.
     *
     * <p>소프트 삭제 여부와 관계없이 레코드 자체를 반환한다.
     * 반환된 레코드의 {@code deletedAt} 값으로 활성/취소 상태를 판별한다.</p>
     *
     * <p>사용 목적: toggleLike 시 기존 레코드 재사용 여부 판단
     * (미존재 → 신규 INSERT, 존재 → UPDATE)</p>
     *
     * @param userId  사용자 ID (VARCHAR 50)
     * @param movieId 영화 ID (VARCHAR 50)
     * @return 좋아요 레코드 (없으면 Optional.empty())
     */
    Optional<Like> findByUserIdAndMovieId(String userId, String movieId);

    /**
     * 활성 좋아요 존재 여부를 확인한다 (deleted_at IS NULL 조건 포함).
     *
     * <p>사용 목적: GET /api/v1/movies/{movieId}/like — 현재 사용자의 좋아요 여부 조회</p>
     *
     * @param userId  사용자 ID (VARCHAR 50)
     * @param movieId 영화 ID (VARCHAR 50)
     * @return 활성 좋아요가 존재하면 {@code true}, 없으면 {@code false}
     */
    boolean existsByUserIdAndMovieIdAndDeletedAtIsNull(String userId, String movieId);

    /**
     * 특정 영화의 활성 좋아요 수를 반환한다 (deleted_at IS NULL 조건 포함).
     *
     * <p>사용 목적: GET /api/v1/movies/{movieId}/like/count — 공개 좋아요 카운트 조회</p>
     *
     * @param movieId 영화 ID (VARCHAR 50)
     * @return 활성 좋아요 수
     */
    long countByMovieIdAndDeletedAtIsNull(String movieId);
}
