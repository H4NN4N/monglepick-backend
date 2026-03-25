package com.monglepick.monglepickbackend.domain.search.entity;

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

import java.time.LocalDateTime;

/**
 * 인기 검색어 엔티티 — trending_keywords 테이블 매핑.
 *
 * <p>전체 사용자의 검색 키워드를 집계하여 인기 검색어 순위를 관리한다.
 * 각 키워드는 고유하며(UNIQUE), 검색될 때마다 카운트가 증가한다.</p>
 *
 * <h3>변경 이력</h3>
 * <ul>
 *   <li>2026-03-24: BaseAuditEntity 상속 추가 (created_at/updated_at/created_by/updated_by 자동 관리)</li>
 *   <li>2026-03-24: PK 필드명 id → trendingKeywordId 로 변경, @Column(name = "trending_keyword_id") 추가</li>
 *   <li>2026-03-24: lastSearchedAt의 @CreationTimestamp 제거 — 도메인 고유 타임스탬프로 유지</li>
 * </ul>
 *
 * <h3>주요 필드</h3>
 * <ul>
 *   <li>{@code keyword} — 검색 키워드 (UNIQUE, 최대 200자)</li>
 *   <li>{@code searchCount} — 전체 검색 횟수 (기본값: 0)</li>
 *   <li>{@code lastSearchedAt} — 마지막 검색 시각 (도메인 고유 타임스탬프)</li>
 * </ul>
 *
 * <h3>활용</h3>
 * <p>검색 화면에서 "인기 검색어 TOP 10" 등을 표시할 때
 * search_count DESC로 정렬하여 조회한다.</p>
 */
@Entity
@Table(name = "trending_keywords")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
/* BaseAuditEntity 상속 추가: created_at, updated_at, created_by, updated_by 컬럼 자동 관리 */
public class TrendingKeyword extends BaseAuditEntity {

    /**
     * 인기 검색어 고유 ID (BIGINT AUTO_INCREMENT PK).
     * 기존 필드명 'id'에서 'trendingKeywordId'로 변경하여 엔티티 식별 명확화.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trending_keyword_id")
    private Long trendingKeywordId;

    /** 검색 키워드 (UNIQUE, 최대 200자, 필수) */
    @Column(name = "keyword", length = 200, nullable = false, unique = true)
    private String keyword;

    /** 전체 검색 횟수 (기본값: 0) */
    @Column(name = "search_count", nullable = false)
    @Builder.Default
    private Integer searchCount = 0;

    /**
     * 마지막 검색 시각 (도메인 고유 타임스탬프).
     * 키워드가 검색될 때마다 갱신된다.
     * BaseAuditEntity의 updated_at과는 별도로, 실제 마지막 검색 시점을 기록하는 도메인 필드.
     * 서비스 레이어에서 직접 설정/갱신한다.
     */
    @Column(name = "last_searched_at")
    private LocalDateTime lastSearchedAt;
}
