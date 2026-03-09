package com.monglepick.monglepickbackend.dto.response;

import com.monglepick.monglepickbackend.entity.Post;

import java.time.LocalDateTime;

/**
 * 게시글 응답 DTO
 *
 * <p>게시글 목록 및 상세 조회 시 반환되는 데이터입니다.</p>
 *
 * @param id 게시글 ID
 * @param title 게시글 제목
 * @param content 게시글 본문
 * @param category 게시글 카테고리
 * @param author 작성자 닉네임
 * @param viewCount 조회수
 * @param createdAt 작성 시각
 */
public record PostResponse(
        Long id,
        String title,
        String content,
        String category,
        String author,
        int viewCount,
        LocalDateTime createdAt
) {
    /**
     * Post 엔티티로부터 PostResponse를 생성하는 팩토리 메서드
     *
     * @param post Post 엔티티 (user가 페치되어 있어야 함)
     * @return PostResponse 인스턴스
     */
    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getCategory().name(),
                post.getUser().getNickname(),
                post.getViewCount(),
                post.getCreatedAt()
        );
    }
}
