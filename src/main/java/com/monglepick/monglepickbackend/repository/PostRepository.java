package com.monglepick.monglepickbackend.repository;

import com.monglepick.monglepickbackend.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 게시글 JPA 리포지토리
 *
 * <p>MySQL posts 테이블에 대한 데이터 접근 레이어입니다.
 * 카테고리별 게시글 목록 조회, 페이징 처리를 지원합니다.</p>
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * 카테고리별 게시글 목록을 페이징으로 조회합니다.
     * <p>최신순 정렬은 Pageable에서 지정합니다.</p>
     *
     * @param category 게시글 카테고리 (FREE, DISCUSSION, RECOMMENDATION, NEWS)
     * @param pageable 페이징 및 정렬 정보
     * @return 페이지 단위의 게시글 목록
     */
    Page<Post> findByCategory(Post.Category category, Pageable pageable);

    /**
     * 특정 사용자가 작성한 게시글 목록을 페이징으로 조회합니다.
     * <p>마이페이지에서 내가 쓴 글 목록을 표시할 때 사용됩니다.</p>
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 및 정렬 정보
     * @return 페이지 단위의 게시글 목록
     */
    Page<Post> findByUserId(Long userId, Pageable pageable);
}
