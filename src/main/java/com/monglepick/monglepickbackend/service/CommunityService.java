package com.monglepick.monglepickbackend.service;

import com.monglepick.monglepickbackend.dto.request.PostCreateRequest;
import com.monglepick.monglepickbackend.dto.request.ReviewCreateRequest;
import com.monglepick.monglepickbackend.dto.response.PostResponse;
import com.monglepick.monglepickbackend.dto.response.ReviewResponse;
import com.monglepick.monglepickbackend.entity.Post;
import com.monglepick.monglepickbackend.entity.Review;
import com.monglepick.monglepickbackend.entity.User;
import com.monglepick.monglepickbackend.exception.BusinessException;
import com.monglepick.monglepickbackend.exception.ErrorCode;
import com.monglepick.monglepickbackend.repository.PostRepository;
import com.monglepick.monglepickbackend.repository.ReviewRepository;
import com.monglepick.monglepickbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 커뮤니티 서비스
 *
 * <p>게시글과 리뷰의 CRUD 비즈니스 로직을 처리합니다.
 * 게시글 작성/수정/삭제 시 작성자 검증을 수행합니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityService {

    private final PostRepository postRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    // ===== 게시글 관련 메서드 =====

    /**
     * 게시글을 작성합니다.
     *
     * @param request 게시글 작성 요청 (제목, 내용, 카테고리)
     * @param userId 작성자 ID (JWT에서 추출)
     * @return 생성된 게시글 응답 DTO
     */
    @Transactional
    public PostResponse createPost(PostCreateRequest request, Long userId) {
        // 1. 작성자 조회
        User user = findUserById(userId);

        // 2. 카테고리 문자열을 Enum으로 변환
        Post.Category category = Post.Category.valueOf(request.category().toUpperCase());

        // 3. 게시글 엔티티 생성 및 저장
        Post post = Post.builder()
                .user(user)
                .title(request.title())
                .content(request.content())
                .category(category)
                .build();

        Post savedPost = postRepository.save(post);
        log.info("게시글 작성 완료 - postId: {}, userId: {}, category: {}",
                savedPost.getId(), userId, category);

        return PostResponse.from(savedPost);
    }

    /**
     * 게시글 상세를 조회합니다. 조회 시 조회수가 1 증가합니다.
     *
     * @param postId 게시글 ID
     * @return 게시글 응답 DTO
     * @throws BusinessException 게시글을 찾을 수 없는 경우
     */
    @Transactional
    public PostResponse getPost(Long postId) {
        Post post = findPostById(postId);
        // 조회수 증가
        post.incrementViewCount();
        return PostResponse.from(post);
    }

    /**
     * 카테고리별 게시글 목록을 조회합니다.
     *
     * @param category 게시글 카테고리 (null이면 전체 조회)
     * @param pageable 페이징 및 정렬 정보
     * @return 페이지 단위의 게시글 목록
     */
    public Page<PostResponse> getPosts(String category, Pageable pageable) {
        if (category != null && !category.isBlank()) {
            // 카테고리 필터링 조회
            Post.Category cat = Post.Category.valueOf(category.toUpperCase());
            return postRepository.findByCategory(cat, pageable).map(PostResponse::from);
        }
        // 전체 조회
        return postRepository.findAll(pageable).map(PostResponse::from);
    }

    /**
     * 게시글을 수정합니다.
     *
     * <p>작성자 본인만 수정할 수 있습니다.</p>
     *
     * @param postId 게시글 ID
     * @param request 수정할 내용
     * @param userId 요청자 ID
     * @return 수정된 게시글 응답 DTO
     * @throws BusinessException 게시글 미존재 또는 권한 없음
     */
    @Transactional
    public PostResponse updatePost(Long postId, PostCreateRequest request, Long userId) {
        Post post = findPostById(postId);

        // 작성자 본인 확인
        validatePostOwner(post, userId);

        // 게시글 내용 수정
        Post.Category category = Post.Category.valueOf(request.category().toUpperCase());
        post.update(request.title(), request.content(), category);

        log.info("게시글 수정 완료 - postId: {}, userId: {}", postId, userId);
        return PostResponse.from(post);
    }

    /**
     * 게시글을 삭제합니다.
     *
     * <p>작성자 본인만 삭제할 수 있습니다.</p>
     *
     * @param postId 게시글 ID
     * @param userId 요청자 ID
     * @throws BusinessException 게시글 미존재 또는 권한 없음
     */
    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = findPostById(postId);
        validatePostOwner(post, userId);

        postRepository.delete(post);
        log.info("게시글 삭제 완료 - postId: {}, userId: {}", postId, userId);
    }

    // ===== 리뷰 관련 메서드 =====

    /**
     * 영화 리뷰를 작성합니다.
     *
     * <p>같은 사용자가 같은 영화에 중복 리뷰를 작성할 수 없습니다.</p>
     *
     * @param request 리뷰 작성 요청 (영화 ID, 평점, 내용)
     * @param userId 작성자 ID
     * @return 생성된 리뷰 응답 DTO
     * @throws BusinessException 중복 리뷰인 경우
     */
    @Transactional
    public ReviewResponse createReview(ReviewCreateRequest request, Long userId) {
        // 1. 중복 리뷰 검사
        if (reviewRepository.existsByUserIdAndMovieId(userId, request.movieId())) {
            log.warn("리뷰 작성 실패 - 중복 리뷰: userId={}, movieId={}",
                    userId, request.movieId());
            throw new BusinessException(ErrorCode.DUPLICATE_REVIEW);
        }

        // 2. 작성자 조회
        User user = findUserById(userId);

        // 3. 리뷰 엔티티 생성 및 저장
        Review review = Review.builder()
                .user(user)
                .movieId(request.movieId())
                .rating(request.rating())
                .content(request.content())
                .build();

        Review savedReview = reviewRepository.save(review);
        log.info("리뷰 작성 완료 - reviewId: {}, userId: {}, movieId: {}",
                savedReview.getId(), userId, request.movieId());

        return ReviewResponse.from(savedReview);
    }

    /**
     * 특정 영화의 리뷰 목록을 조회합니다.
     *
     * @param movieId 영화 ID
     * @return 리뷰 목록
     */
    public List<ReviewResponse> getReviewsByMovie(Long movieId) {
        return reviewRepository.findByMovieId(movieId).stream()
                .map(ReviewResponse::from)
                .toList();
    }

    // ===== 헬퍼 메서드 =====

    /** 사용자 ID로 사용자를 조회하는 헬퍼 */
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /** 게시글 ID로 게시글을 조회하는 헬퍼 */
    private Post findPostById(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }

    /** 게시글 작성자와 요청자가 일치하는지 검증하는 헬퍼 */
    private void validatePostOwner(Post post, Long userId) {
        if (!post.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.POST_ACCESS_DENIED);
        }
    }
}
