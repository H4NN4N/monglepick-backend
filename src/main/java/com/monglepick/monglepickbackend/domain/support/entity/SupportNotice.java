package com.monglepick.monglepickbackend.domain.support.entity;

import com.monglepick.monglepickbackend.global.entity.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 공지사항 통합 엔티티 — support_notices 테이블 매핑.
 *
 * <p>2026-04-08 개편: 구 AppNotice(앱 메인 BANNER/POPUP/MODAL)를 본 엔티티로 흡수.
 * 고객센터 공지 + 앱 메인 공지가 하나의 "Notice" 도메인으로 통합되었다.
 * (단일 진실 원본 원칙 — 이중 관리 해소)</p>
 *
 * <h3>노출 방식 (display_type)</h3>
 * <ul>
 *   <li>{@code LIST_ONLY} — 고객센터 공지 목록에만 노출 (기본값, 기존 SupportNotice 동작)</li>
 *   <li>{@code BANNER} — 앱 홈 배너에 추가 노출</li>
 *   <li>{@code POPUP} — 앱 시작 시 팝업에 추가 노출</li>
 *   <li>{@code MODAL} — 중요 공지 모달 (강제 확인)</li>
 * </ul>
 *
 * <h3>콘텐츠 카테고리 (notice_type)</h3>
 * <ul>
 *   <li>{@code NOTICE} — 일반 공지 (기본값)</li>
 *   <li>{@code UPDATE} — 업데이트/릴리스 노트</li>
 *   <li>{@code MAINTENANCE} — 서비스 점검 안내</li>
 *   <li>{@code EVENT} — 이벤트 공지</li>
 * </ul>
 *
 * <h3>앱 메인 노출 조건 (BANNER/POPUP/MODAL 전용)</h3>
 * <ol>
 *   <li>{@code is_active = true}</li>
 *   <li>{@code start_at <= NOW() OR start_at IS NULL} (시작일 도달)</li>
 *   <li>{@code end_at >= NOW() OR end_at IS NULL} (종료일 미도달)</li>
 * </ol>
 *
 * <h3>주요 필드</h3>
 * <ul>
 *   <li>{@code title} — 공지 제목 (필수, 최대 200자)</li>
 *   <li>{@code content} — 공지 본문 (마크다운/HTML 허용, TEXT)</li>
 *   <li>{@code noticeType} — 콘텐츠 카테고리 코드</li>
 *   <li>{@code displayType} — 노출 방식 (LIST_ONLY/BANNER/POPUP/MODAL)</li>
 *   <li>{@code isPinned} — 목록 상단 고정 여부 (기본 false)</li>
 *   <li>{@code sortOrder} — 고정 공지 순서 (낮은 값이 상위)</li>
 *   <li>{@code publishedAt} — 공개 시각 (nullable, null 이면 초안)</li>
 *   <li>{@code linkUrl} — 배너 클릭 시 이동 URL (nullable, 구 AppNotice 흡수)</li>
 *   <li>{@code imageUrl} — 배너/팝업 이미지 URL (nullable, 구 AppNotice 흡수)</li>
 *   <li>{@code startAt} — 노출 시작 시각 (nullable, 구 AppNotice 흡수)</li>
 *   <li>{@code endAt} — 노출 종료 시각 (nullable, 구 AppNotice 흡수)</li>
 *   <li>{@code priority} — 정렬 우선순위 (기본 0, 구 AppNotice 흡수)</li>
 *   <li>{@code isActive} — 앱 메인 노출 활성 토글 (기본 true, 구 AppNotice 흡수)</li>
 * </ul>
 */
@Entity
@Table(
        name = "support_notices",
        indexes = {
                /* 최신순 노출을 위한 created_at 인덱스 */
                @Index(name = "idx_support_notices_created_at", columnList = "created_at"),
                /* 상단 고정 공지 우선 정렬을 위한 pinned + sort_order 조합 인덱스 */
                @Index(name = "idx_support_notices_pinned_order", columnList = "is_pinned, sort_order"),
                /* 유형별 필터링 */
                @Index(name = "idx_support_notices_type", columnList = "notice_type"),
                /* 앱 메인 노출 필터링용 (구 AppNotice 흡수) */
                @Index(name = "idx_support_notices_display_active", columnList = "display_type, is_active"),
                @Index(name = "idx_support_notices_period", columnList = "start_at, end_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SupportNotice extends BaseAuditEntity {

    /** 공지사항 고유 ID (BIGINT AUTO_INCREMENT PK). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notice_id")
    private Long noticeId;

    /** 공지 제목 (VARCHAR(200), 필수). */
    @Column(name = "title", length = 200, nullable = false)
    private String title;

    /** 공지 본문 (TEXT, 필수). 마크다운/HTML 허용. */
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * 콘텐츠 카테고리 (VARCHAR(30), 기본값: NOTICE).
     * NOTICE / UPDATE / MAINTENANCE / EVENT 중 하나.
     */
    @Column(name = "notice_type", length = 30, nullable = false)
    @Builder.Default
    private String noticeType = "NOTICE";

    /**
     * 노출 방식 (VARCHAR(20), 기본값: LIST_ONLY).
     *
     * <p>LIST_ONLY = 고객센터 공지 목록에만 노출 (기존 동작).<br>
     * BANNER/POPUP/MODAL = 앱 메인 화면에도 추가 노출 (구 AppNotice 기능).</p>
     */
    @Column(name = "display_type", length = 20, nullable = false)
    @Builder.Default
    private String displayType = "LIST_ONLY";

    /** 상단 고정 여부 (기본값: false). */
    @Column(name = "is_pinned", nullable = false)
    @Builder.Default
    private Boolean isPinned = false;

    /**
     * 정렬 순서 (nullable).
     * 고정 공지끼리의 정렬에 사용한다. 낮은 값일수록 상위에 노출된다.
     */
    @Column(name = "sort_order")
    private Integer sortOrder;

    /**
     * 공개 시각 (nullable).
     * null 이면 초안 상태. 값이 지정되면 해당 시각 이후 사용자에게 노출된다.
     */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    // ─────────────────────────────────────────────
    // 앱 메인 노출 필드 (2026-04-08: 구 AppNotice 흡수)
    // ─────────────────────────────────────────────

    /**
     * 링크 URL (nullable).
     *
     * <p>BANNER/POPUP 클릭 시 이동할 URL. 외부 링크 또는 앱 내 딥링크.
     * LIST_ONLY 공지에는 일반적으로 사용하지 않음.</p>
     */
    @Column(name = "link_url", length = 500)
    private String linkUrl;

    /**
     * 이미지 URL (nullable).
     *
     * <p>BANNER/POPUP에 함께 노출할 이미지. S3 또는 CDN URL.</p>
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** 앱 메인 노출 시작 시각 (nullable — null이면 즉시). */
    @Column(name = "start_at")
    private LocalDateTime startAt;

    /** 앱 메인 노출 종료 시각 (nullable — null이면 무기한). */
    @Column(name = "end_at")
    private LocalDateTime endAt;

    /**
     * 정렬 우선순위 (기본 0, 높을수록 상단).
     *
     * <p>동시에 노출되는 여러 BANNER의 표시 순서 결정.</p>
     */
    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Integer priority = 0;

    /**
     * 앱 메인 노출 활성 토글 (기본 true).
     *
     * <p>false 이면 기간/displayType과 무관하게 앱 메인 노출이 차단된다.
     * 고객센터 목록(LIST_ONLY)은 publishedAt 기준이므로 본 플래그의 영향을 받지 않는다.</p>
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // ─────────────────────────────────────────────
    // 도메인 메서드
    // ─────────────────────────────────────────────

    /**
     * 공지 메타 정보 전체 수정 (기본 + 앱 메인 노출 필드 통합).
     */
    public void updateAll(String title,
                          String content,
                          String noticeType,
                          String displayType,
                          Boolean isPinned,
                          Integer sortOrder,
                          LocalDateTime publishedAt,
                          String linkUrl,
                          String imageUrl,
                          LocalDateTime startAt,
                          LocalDateTime endAt,
                          Integer priority,
                          Boolean isActive) {
        if (title != null) this.title = title;
        if (content != null) this.content = content;
        if (noticeType != null && !noticeType.isBlank()) this.noticeType = noticeType;
        if (displayType != null && !displayType.isBlank()) this.displayType = displayType;
        if (isPinned != null) this.isPinned = isPinned;
        if (sortOrder != null) this.sortOrder = sortOrder;
        if (publishedAt != null) this.publishedAt = publishedAt;
        this.linkUrl = linkUrl;
        this.imageUrl = imageUrl;
        this.startAt = startAt;
        this.endAt = endAt;
        if (priority != null) this.priority = priority;
        if (isActive != null) this.isActive = isActive;
    }

    /** 상단 고정 설정/해제. */
    public void setPinned(boolean pinned) {
        this.isPinned = pinned;
    }

    /** 정렬 순서 변경. */
    public void updateSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    /** 공개 시각 설정. */
    public void publish(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    /** 앱 메인 노출 활성 토글 (구 AppNotice.updateActive 흡수). */
    public void updateActive(boolean active) {
        this.isActive = active;
    }
}
