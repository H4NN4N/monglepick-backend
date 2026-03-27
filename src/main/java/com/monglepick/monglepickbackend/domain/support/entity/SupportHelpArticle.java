package com.monglepick.monglepickbackend.domain.support.entity;

import com.monglepick.monglepickbackend.global.entity.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 고객센터 도움말 문서 엔티티.
 *
 * <p>MySQL {@code support_help_articles} 테이블과 매핑된다.
 * FAQ보다 상세한 기능 안내, 사용법 가이드, 공지사항 등을 저장한다.</p>
 *
 * <h3>조회수 카운터</h3>
 * <p>{@code view_count}는 문서를 조회할 때마다 1씩 증가하며,
 * {@link SupportHelpArticleRepository}의 JPQL @Modifying 쿼리로 직접 업데이트된다.
 * 이를 통해 전체 엔티티를 재로딩하지 않고도 카운터를 증가시킬 수 있다.</p>
 */
@Entity
@Table(name = "support_help_articles", indexes = {
        // 카테고리별 문서 목록 조회 시 사용
        @Index(name = "idx_support_help_category", columnList = "category"),
        // 최신순 정렬 시 사용
        @Index(name = "idx_support_help_created_at", columnList = "created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupportHelpArticle extends BaseAuditEntity {

    /**
     * 도움말 문서 고유 식별자 (BIGINT AUTO_INCREMENT PK).
     * DB가 자동 생성하며 변경 불가.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "article_id")
    private Long articleId;

    /**
     * 문서 카테고리.
     * GENERAL, ACCOUNT, CHAT, RECOMMENDATION, COMMUNITY, PAYMENT 중 하나.
     * EnumType.STRING으로 DB에 문자열로 저장된다.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SupportCategory category;

    /**
     * 문서 제목 (VARCHAR 200).
     * "AI 채팅 기능 사용 가이드" 형태의 간결한 제목.
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * 문서 본문 (TEXT).
     * 마크다운 또는 HTML 형식의 상세 안내 내용.
     * 스크린샷 URL, 링크 등이 포함될 수 있다.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 조회수 (기본값 0).
     * 문서 상세 조회 시 SupportHelpArticleRepository의 @Modifying 쿼리로 직접 증가된다.
     * 동시성 충돌을 피하기 위해 엔티티 전체 재로딩 없이 DB UPDATE로 처리한다.
     */
    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    /**
     * 생성자 (빌더 패턴).
     *
     * <p>viewCount는 항상 0으로 초기화된다.</p>
     *
     * @param category 카테고리
     * @param title    제목
     * @param content  본문
     */
    @Builder
    public SupportHelpArticle(SupportCategory category, String title, String content) {
        this.category = category;
        this.title = title;
        this.content = content;
        this.viewCount = 0;
    }

    // ─────────────────────────────────────────────
    // 도메인 메서드
    // ─────────────────────────────────────────────

    /**
     * 제목/본문/카테고리 수정.
     *
     * @param category 변경할 카테고리
     * @param title    변경할 제목
     * @param content  변경할 본문
     */
    public void update(SupportCategory category, String title, String content) {
        this.category = category;
        this.title = title;
        this.content = content;
    }

    /**
     * 조회수 1 증가 (인메모리 증가용).
     *
     * <p>주의: 서비스에서는 DB 직접 UPDATE 방식(@Modifying 쿼리)을 권장한다.
     * 이 메서드는 단일 인스턴스 테스트 또는 배치 처리 시에만 사용한다.</p>
     */
    public void incrementViewCount() {
        this.viewCount++;
    }
}
