package com.monglepick.monglepickbackend.domain.watchhistory.controller;

import com.monglepick.monglepickbackend.domain.watchhistory.dto.WatchHistoryRequest;
import com.monglepick.monglepickbackend.domain.watchhistory.dto.WatchHistoryResponse;
import com.monglepick.monglepickbackend.domain.watchhistory.service.WatchHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 시청이력 전용 컨트롤러.
 *
 * <p>기존 {@code UserController}의 {@code GET /api/v1/users/me/watch-history}는
 * 마이페이지 맥락에서 그대로 유지하고, 이 컨트롤러는 시청이력의
 * <b>독립 경로(/api/v1/watch-history)</b>에서 추가·조회·삭제를 전담합니다.</p>
 *
 * <h3>경로 분리 이유</h3>
 * <ul>
 *   <li>마이페이지({@code /users/me/watch-history}): 프로필과 묶어 한 번에 렌더링하는 용도</li>
 *   <li>시청이력 전용({@code /watch-history}): 기록 추가·삭제, 독립적인 이력 탐색 용도</li>
 * </ul>
 *
 * <h3>인증</h3>
 * <p>모든 엔드포인트는 JWT Bearer 인증 필수입니다.
 * SecurityConfig의 기본 정책(authenticated)에 의해 자동 보호됩니다.</p>
 */
@Tag(name = "시청이력", description = "시청 기록 추가·조회·삭제 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/watch-history")
@RequiredArgsConstructor
public class WatchHistoryController {

    private final WatchHistoryService watchHistoryService;

    // ════════════════════════════════════════════════════════════════
    // POST /api/v1/watch-history — 시청 기록 추가
    // ════════════════════════════════════════════════════════════════

    /**
     * 시청 기록 추가 API.
     *
     * <p>사용자가 영화를 시청한 기록을 저장합니다.
     * 동일한 영화를 여러 번 시청한 경우 중복 기록이 허용됩니다(AI 추천 정확도 향상 목적).
     * {@code watchedAt}을 생략하면 서버 수신 시각이 자동으로 설정됩니다.</p>
     *
     * <h3>요청 예시</h3>
     * <pre>{@code
     * POST /api/v1/watch-history
     * Authorization: Bearer {accessToken}
     * {
     *   "movieId": "tt1375666",
     *   "watchedAt": "2026-03-26T21:00:00",  // 생략 가능
     *   "rating": 4.5                          // 생략 가능
     * }
     * }</pre>
     *
     * @param userId  JWT 토큰에서 추출한 사용자 ID
     * @param request 시청 기록 요청 DTO (movieId 필수, watchedAt·rating 선택)
     * @return 201 Created + 저장된 시청이력 DTO
     */
    @Operation(
            summary = "시청 기록 추가",
            description = "사용자가 영화를 시청한 기록을 저장합니다. watchedAt 생략 시 현재 시각, rating 생략 시 미평가로 처리됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "시청 기록 추가 성공"),
            @ApiResponse(responseCode = "400", description = "요청 유효성 검증 실패 (movieId 누락, rating 범위 오류 등)"),
            @ApiResponse(responseCode = "401", description = "JWT 인증 실패"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping
    public ResponseEntity<WatchHistoryResponse> addWatchHistory(
            @AuthenticationPrincipal String userId,
            @RequestBody @Valid WatchHistoryRequest request) {

        log.info("시청 기록 추가 요청 - userId: {}, movieId: {}", userId, request.movieId());
        WatchHistoryResponse response = watchHistoryService.addWatchHistory(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ════════════════════════════════════════════════════════════════
    // GET /api/v1/watch-history — 내 시청이력 목록 (페이징)
    // ════════════════════════════════════════════════════════════════

    /**
     * 내 시청이력 목록 조회 API.
     *
     * <p>로그인한 사용자의 시청이력을 최신순으로 페이징 조회합니다.
     * watch_history 테이블은 26M+ 행의 대용량 테이블이므로 반드시 페이징을 사용합니다.</p>
     *
     * <h3>페이징 파라미터 (쿼리스트링)</h3>
     * <ul>
     *   <li>{@code page} — 페이지 번호 (0부터 시작, 기본값: 0)</li>
     *   <li>{@code size} — 페이지 크기 (기본값: 20)</li>
     *   <li>{@code sort} — 정렬 기준 (기본값: watchedAt,desc)</li>
     * </ul>
     *
     * <h3>요청 예시</h3>
     * <pre>{@code
     * GET /api/v1/watch-history?page=0&size=20&sort=watchedAt,desc
     * Authorization: Bearer {accessToken}
     * }</pre>
     *
     * @param userId   JWT 토큰에서 추출한 사용자 ID
     * @param pageable 페이징·정렬 정보 (기본: 20건, watchedAt 역순)
     * @return 200 OK + 페이지 단위의 시청이력 DTO
     */
    @Operation(
            summary = "내 시청이력 목록 조회",
            description = "로그인한 사용자의 시청이력을 최신순으로 페이징 조회합니다. page/size/sort 쿼리 파라미터로 제어합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "시청이력 조회 성공"),
            @ApiResponse(responseCode = "401", description = "JWT 인증 실패")
    })
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping
    public ResponseEntity<Page<WatchHistoryResponse>> getWatchHistory(
            @AuthenticationPrincipal String userId,
            @PageableDefault(size = 20, sort = "watchedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        log.debug("시청이력 목록 조회 - userId: {}, page: {}", userId, pageable.getPageNumber());
        Page<WatchHistoryResponse> history = watchHistoryService.getWatchHistory(userId, pageable);
        return ResponseEntity.ok(history);
    }

    // ════════════════════════════════════════════════════════════════
    // DELETE /api/v1/watch-history/{id} — 시청 기록 삭제
    // ════════════════════════════════════════════════════════════════

    /**
     * 시청 기록 삭제 API.
     *
     * <p>특정 시청 기록을 삭제합니다. 본인의 기록만 삭제할 수 있습니다.
     * 타인의 기록 삭제 시도는 400으로 거부합니다.</p>
     *
     * <h3>요청 예시</h3>
     * <pre>{@code
     * DELETE /api/v1/watch-history/42
     * Authorization: Bearer {accessToken}
     * }</pre>
     *
     * @param userId         JWT 토큰에서 추출한 사용자 ID
     * @param watchHistoryId 삭제할 시청이력 PK
     * @return 204 No Content
     */
    @Operation(
            summary = "시청 기록 삭제",
            description = "특정 시청 기록을 삭제합니다. 본인의 기록만 삭제 가능하며, 타인의 기록 삭제 시도는 400으로 거부됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "시청 기록 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "해당 기록 없음 또는 소유권 불일치"),
            @ApiResponse(responseCode = "401", description = "JWT 인증 실패")
    })
    @SecurityRequirement(name = "BearerAuth")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWatchHistory(
            @AuthenticationPrincipal String userId,
            @Parameter(description = "삭제할 시청이력 ID (watch_history_id)", example = "42")
            @PathVariable("id") Long watchHistoryId) {

        log.info("시청 기록 삭제 요청 - userId: {}, watchHistoryId: {}", userId, watchHistoryId);
        watchHistoryService.deleteWatchHistory(userId, watchHistoryId);
        return ResponseEntity.noContent().build();
    }
}
