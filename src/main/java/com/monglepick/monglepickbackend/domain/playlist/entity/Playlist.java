package com.monglepick.monglepickbackend.domain.playlist.entity;

/* BaseAuditEntity: created_at, updated_at, created_by, updated_by 자동 관리 */
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

/**
 * 플레이리스트 엔티티 — playlist 테이블 매핑.
 *
 * <p>사용자가 생성한 영화 플레이리스트(큐레이션)를 저장한다.
 * 공개/비공개 설정을 지원하며, 플레이리스트에 포함된 영화는
 * {@link PlaylistItem}에서 관리한다.</p>
 *
 * <h3>변경 이력</h3>
 * <ul>
 *   <li>2026-03-24: BaseTimeEntity → BaseAuditEntity 변경 (created_by/updated_by 추가)</li>
 * </ul>
 *
 * <h3>주요 필드</h3>
 * <ul>
 *   <li>{@code userId} — 소유자 사용자 ID</li>
 *   <li>{@code playlistName} — 플레이리스트 이름 (필수)</li>
 *   <li>{@code description} — 플레이리스트 설명 (선택)</li>
 *   <li>{@code isPublic} — 공개 여부 (기본값: false)</li>
 * </ul>
 */
@Entity
@Table(name = "playlist")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
/* BaseTimeEntity → BaseAuditEntity 변경: created_by, updated_by 컬럼 추가 관리 */
public class Playlist extends BaseAuditEntity {

    /** 플레이리스트 고유 ID (BIGINT AUTO_INCREMENT PK) — PK 컬럼명 변경 없음 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "playlist_id")
    private Long playlistId;

    /**
     * 소유자 사용자 ID (VARCHAR(50), NOT NULL).
     * users.user_id를 참조한다.
     */
    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    /** 플레이리스트 이름 (필수, 최대 200자) */
    @Column(name = "playlist_name", length = 200, nullable = false)
    private String playlistName;

    /** 플레이리스트 설명 (TEXT 타입, 선택) */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 공개 여부.
     * 기본값: false (비공개).
     * true로 설정하면 다른 사용자도 이 플레이리스트를 볼 수 있다.
     */
    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = false;
}
