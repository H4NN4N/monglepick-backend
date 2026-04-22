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
 * 사용자 선호 장르 엔티티 — fav_genre 테이블 매핑.
 *
 * <p>사용자가 선호하는 영화 장르를 우선순위와 함께 저장한다.
 * 온보딩(이상형 월드컵) 또는 프로필 설정에서 수집된다.</p>
 *
 * <h3>주요 필드</h3>
 * <ul>
 *   <li>{@code userId} — 사용자 ID</li>
 *   <li>{@code genreId} — {@code genre_master.genre_id}를 저장하는 내부 장르 ID</li>
 *   <li>{@code priority} — 우선순위 (0이 가장 높음)</li>
 * </ul>
 *
 * <h3>제약조건</h3>
 * <p>UNIQUE(user_id, genre_name) — 동일 사용자가 동일 장르 ID를 중복 등록할 수 없다.</p>
 */
@Entity
@Table(
        name = "fav_genre",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "genre_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
/**
 * BaseTimeEntity → BaseAuditEntity로 변경: created_by, updated_by 감사 필드 추가 상속
 * — PK 필드명: id → favGenreId로 변경 (DDL 컬럼명 fav_genre_id 매핑)
 */
public class FavGenre extends BaseAuditEntity {

    /** 선호 장르 레코드 고유 ID (PK, BIGINT AUTO_INCREMENT, 컬럼명: fav_genre_id) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fav_genre_id")
    private Long favGenreId;

    /**
     * 사용자 ID (VARCHAR(50), NOT NULL).
     * users.user_id를 참조한다.
     */
    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    /**
     * 장르 ID (VARCHAR(50), NOT NULL, 물리 컬럼명: genre_name).
     *
     * <p>기존 컬럼명은 {@code genre_name}이지만, 이제 의미상으로는
     * {@code genre_master.genre_id}를 저장한다.</p>
     */
    @Column(name = "genre_id", nullable = false)
    private Long genreId;

    /**
     * 우선순위.
     * 기본값: 0.
     * 낮은 숫자일수록 높은 우선순위를 의미한다.
     */
    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 0;
}
