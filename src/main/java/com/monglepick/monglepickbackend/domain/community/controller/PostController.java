package com.monglepick.monglepickbackend.domain.community.controller;

import com.monglepick.monglepickbackend.domain.community.dto.PostCreateRequest;
import com.monglepick.monglepickbackend.domain.community.dto.PostResponse;
import com.monglepick.monglepickbackend.domain.community.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 게시글 컨트롤러
 *
 * <p>커뮤니티 게시글의 CRUD + 임시저장(Draft) API를 제공합니다.
 * Downloads POST 파일의 임시저장/게시 엔드포인트를 통합하였습니다.</p>
 *
 * <h3>API 목록</h3>
 * <ul>
 *   <li>GET /api/v1/posts — 게시글 목록 조회 (비로그인 허용)</li>
 *   <li>GET /api/v1/posts/{id} — 게시글 상세 조회 (비로그인 허용)</li>
 *   <li>POST /api/v1/posts — 게시글 작성 (인증 필요)</li>
 *   <li>PUT /api/v1/posts/{id} — 게시글 수정 (작성자만)</li>
 *   <li>DELETE /api/v1/posts/{id} — 게시글 삭제 (작성자만)</li>
 *   <li>POST /api/v1/posts/drafts — 임시저장 작성 (인증 필요)</li>
 *   <li>GET /api/v1/posts/drafts — 임시저장 목록 (인증 필요)</li>
 *   <li>PUT /api/v1/posts/drafts/{id} — 임시저장 수정 (작성자만)</li>
 *   <li>DELETE /api/v1/posts/drafts/{id} — 임시저장 삭제 (작성자만)</li>
 *   <li>POST /api/v1/posts/drafts/{id}/publish — 임시저장 게시 (작성자만)</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // ──────────────────────────────────────────────
    // 게시글 CRUD (기존 기능)
    // ──────────────────────────────────────────────

    /**
     * 게시글 목록 조회 API (비로그인 허용)
     */
    @GetMapping
    public ResponseEntity<Page<PostResponse>> getPosts(
            @RequestParam(required = false) String category,
            @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<PostResponse> posts = postService.getPosts(category, pageable);
        return ResponseEntity.ok(posts);
    }

    /**
     * 게시글 상세 조회 API (조회수 1 증가)
     */
    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPost(@PathVariable Long id) {
        PostResponse post = postService.getPost(id);
        return ResponseEntity.ok(post);
    }

    /**
     * 게시글 작성 API (인증 필요)
     */
    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody PostCreateRequest request,
            @AuthenticationPrincipal String userId) {

        log.info("게시글 작성 요청 — userId: {}, category: {}", userId, request.category());
        PostResponse post = postService.createPost(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(post);
    }

    /**
     * 게시글 수정 API (작성자만)
     */
    @PutMapping("/{id}")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody PostCreateRequest request,
            @AuthenticationPrincipal String userId) {

        log.info("게시글 수정 요청 — postId: {}, userId: {}", id, userId);
        PostResponse post = postService.updatePost(id, request, userId);
        return ResponseEntity.ok(post);
    }

    /**
     * 게시글 삭제 API (작성자만)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long id,
            @AuthenticationPrincipal String userId) {

        log.info("게시글 삭제 요청 — postId: {}, userId: {}", id, userId);
        postService.deletePost(id, userId);
        return ResponseEntity.noContent().build();
    }

    // ──────────────────────────────────────────────
    // 임시저장 기능 (Downloads POST 파일 적용)
    // ──────────────────────────────────────────────

    /**
     * 임시저장 작성 API (인증 필요)
     */
    @PostMapping("/drafts")
    public ResponseEntity<PostResponse> createDraft(
            @Valid @RequestBody PostCreateRequest request,
            @AuthenticationPrincipal String userId) {

        log.info("임시저장 작성 요청 — userId: {}", userId);
        PostResponse post = postService.createDraft(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(post);
    }

    /**
     * 임시저장 목록 조회 API (인증 필요)
     */
    @GetMapping("/drafts")
    public ResponseEntity<Page<PostResponse>> getDrafts(
            @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            @AuthenticationPrincipal String userId) {

        Page<PostResponse> drafts = postService.getDrafts(userId, pageable);
        return ResponseEntity.ok(drafts);
    }

    /**
     * 임시저장 수정 API (작성자만)
     */
    @PutMapping("/drafts/{id}")
    public ResponseEntity<PostResponse> updateDraft(
            @PathVariable Long id,
            @Valid @RequestBody PostCreateRequest request,
            @AuthenticationPrincipal String userId) {

        PostResponse post = postService.updateDraft(id, request, userId);
        return ResponseEntity.ok(post);
    }

    /**
     * 임시저장 삭제 API (작성자만)
     */
    @DeleteMapping("/drafts/{id}")
    public ResponseEntity<Void> deleteDraft(
            @PathVariable Long id,
            @AuthenticationPrincipal String userId) {

        postService.deleteDraft(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 임시저장 → 게시 API (작성자만)
     */
    @PostMapping("/drafts/{id}/publish")
    public ResponseEntity<PostResponse> publishDraft(
            @PathVariable Long id,
            @AuthenticationPrincipal String userId) {

        PostResponse post = postService.publishDraft(id, userId);
        return ResponseEntity.ok(post);
    }
}
