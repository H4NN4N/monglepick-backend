package com.monglepick.monglepickbackend.domain.movie.entity;

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

/**
 * 사용자 선호 영화 엔티티 — fav_movie 테이블 매핑.
 *
 * <p>사용자가 선호하는(즐겨찾기한) 영화를 우선순위와 함께 저장한다.
 * 온보딩(이상형 월드컵) 또는 프로필 설정에서 수집된다.</p>
 *
 * <h3>주요 필드</h3>
 * <ul>
 *   <li>{@code userId} — 사용자 ID</li>
 *   <li>{@code movieId} — 영화 ID</li>
 *   <li>{@code priority} — 우선순위 (0이 가장 높음)</li>
 * </ul>
 *
 * <h3>제약조건</h3>
 * <p>UNIQUE(user_id, movie_id) — 동일 사용자가 동일 영화를 중복 등록할 수 없다.</p>
 */
@Entity
@Table(
        name = "fav_movie",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "movie_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
/**
 * BaseTimeEntity → BaseAuditEntity로 변경: created_by, updated_by 감사 필드 추가 상속
 * — PK 필드명: id → favMovieId로 변경 (DDL 컬럼명 fav_movie_id 매핑)
 */
public class FavMovie extends BaseAuditEntity {

    /** 선호 영화 레코드 고유 ID (PK, BIGINT AUTO_INCREMENT, 컬럼명: fav_movie_id) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fav_movie_id")
    private Long favMovieId;

    /**
     * 사용자 ID (VARCHAR(50), NOT NULL).
     * users.user_id를 참조한다.
     */
    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    /**
     * 영화 ID (VARCHAR(50), NOT NULL).
     * movies.movie_id를 참조한다.
     */
    @Column(name = "movie_id", length = 50, nullable = false)
    private String movieId;

    /**
     * 우선순위.
     * 기본값: 0.
     * 낮은 숫자일수록 높은 우선순위를 의미한다.
     */
    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 0;
}
