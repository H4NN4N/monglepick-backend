package com.monglepick.monglepickbackend.domain.community.entity;

import com.monglepick.monglepickbackend.domain.user.entity.User;
import com.monglepick.monglepickbackend.global.entity.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 커뮤니티 게시글 엔티티
 *
 * <p>MySQL posts 테이블과 매핑됩니다.
 * 사용자가 작성하는 영화 토론, 자유 게시글, 추천 요청 등을 저장합니다.</p>
 *
 * <p>게시글 카테고리:</p>
 * <ul>
 *   <li>FREE: 자유 게시판</li>
 *   <li>DISCUSSION: 영화 토론</li>
 *   <li>RECOMMENDATION: 추천 요청/공유</li>
 *   <li>NEWS: 영화 뉴스/소식</li>
 * </ul>
 *
 * <h3>임시저장 기능 (Downloads POST 파일 적용)</h3>
 * <ul>
 *   <li>DRAFT: 임시저장 상태 (작성자만 조회 가능)</li>
 *   <li>PUBLISHED: 게시 완료 상태 (전체 공개)</li>
 * </ul>
 */
@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseAuditEntity {

    /** 게시글 고유 식별자 (BIGINT AUTO_INCREMENT PK) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long postId;

    /** 작성자 (지연 로딩) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 게시글 제목 (최대 200자) */
    @Column(nullable = false, length = 200)
    private String title;

    /** 게시글 본문 (TEXT 타입) */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 게시글 카테고리 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Category category;

    /** 조회수 (기본값 0) */
    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    /** 게시글 상태 — 임시저장(DRAFT) / 게시됨(PUBLISHED) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status;

    /**
     * 게시글 카테고리 열거형
     */
    public enum Category {
        /** 자유 게시판 */
        FREE,
        /** 영화 토론 */
        DISCUSSION,
        /** 추천 요청/공유 */
        RECOMMENDATION,
        /** 영화 뉴스/소식 */
        NEWS
    }

    @Builder
    public Post(User user, String title, String content, Category category, PostStatus status) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.category = category;
        this.status = status != null ? status : PostStatus.PUBLISHED;
        this.viewCount = 0;
    }

    /** 게시글 내용 수정 */
    public void update(String title, String content, Category category) {
        this.title = title;
        this.content = content;
        this.category = category;
    }

    /** 조회수 1 증가 */
    public void incrementViewCount() {
        this.viewCount++;
    }

    /** 임시저장 → 게시글 업로드 */
    public void publish() {
        this.status = PostStatus.PUBLISHED;
    }
}
