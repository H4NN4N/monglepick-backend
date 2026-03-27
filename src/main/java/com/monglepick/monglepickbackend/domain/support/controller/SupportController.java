package com.monglepick.monglepickbackend.domain.support.controller;

import com.monglepick.monglepickbackend.domain.support.dto.SupportDto.FaqFeedbackRequest;
import com.monglepick.monglepickbackend.domain.support.dto.SupportDto.FaqResponse;
import com.monglepick.monglepickbackend.domain.support.dto.SupportDto.HelpArticleResponse;
import com.monglepick.monglepickbackend.domain.support.dto.SupportDto.TicketCreateRequest;
import com.monglepick.monglepickbackend.domain.support.dto.SupportDto.TicketResponse;
import com.monglepick.monglepickbackend.domain.support.service.SupportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 고객센터(Support) REST 컨트롤러.
 *
 * <p>FAQ 조회/피드백, 도움말 조회, 상담 티켓 생성/조회 API를 제공한다.
 * 프론트엔드의 {@code /support} 페이지(4개 탭)에 대응한다.</p>
 *
 * <h3>엔드포인트 요약</h3>
 * <ul>
 *   <li>{@code GET  /api/v1/support/faq}             — FAQ 목록 (비인증)</li>
 *   <li>{@code POST /api/v1/support/faq/{id}/feedback} — FAQ 피드백 (인증)</li>
 *   <li>{@code GET  /api/v1/support/help}             — 도움말 목록 (비인증)</li>
 *   <li>{@code POST /api/v1/support/tickets}          — 티켓 생성 (인증)</li>
 *   <li>{@code GET  /api/v1/support/tickets}          — 내 티켓 목록 (인증)</li>
 * </ul>
 */
@Tag(name = "고객센터", description = "FAQ, 도움말, 상담 티켓 API")
@RestController
@RequestMapping("/api/v1/support")
@RequiredArgsConstructor
public class SupportController {

    private final SupportService supportService;

    // ─────────────────────────────────────────────
    // FAQ
    // ─────────────────────────────────────────────

    /**
     * FAQ 목록을 조회한다.
     *
     * <p>category 파라미터가 없으면 전체 FAQ를 최신순으로 반환한다.
     * 비로그인 사용자도 조회할 수 있다.</p>
     *
     * @param category 필터 카테고리 (선택) — GENERAL, ACCOUNT, CHAT, RECOMMENDATION, COMMUNITY, PAYMENT
     * @return FAQ 목록 (200 OK)
     */
    @Operation(summary = "FAQ 목록 조회", description = "카테고리별 또는 전체 FAQ를 조회한다 (비인증)")
    @GetMapping("/faq")
    public ResponseEntity<List<FaqResponse>> getFaqs(
            @Parameter(description = "카테고리 필터 (선택)")
            @RequestParam(required = false) String category
    ) {
        return ResponseEntity.ok(supportService.getFaqs(category));
    }

    /**
     * FAQ에 ���드백을 제출한다.
     *
     * <p>로그인한 사용자만 가능하며, 동일 FAQ에 중복 피드백은 ��가하다.</p>
     *
     * @param faqId   피드백 대상 FAQ ID
     * @param userId  JWT에서 추출한 사용자 ID
     * @param request 피드백 요청 (helpful: true/false)
     * @return 성공 시 200 OK
     */
    @Operation(summary = "FAQ 피드백 제출", description = "FAQ에 도움됨/도움 안됨 피드백을 제출한다 (인증 필수)")
    @PostMapping("/faq/{faqId}/feedback")
    public ResponseEntity<Void> submitFaqFeedback(
            @PathVariable Long faqId,
            @AuthenticationPrincipal String userId,
            @RequestBody FaqFeedbackRequest request
    ) {
        supportService.submitFaqFeedback(faqId, userId, request);
        return ResponseEntity.ok().build();
    }

    // ─────────────────────────────────────────────
    // 도움말
    // ─────────────────────────────────────────────

    /**
     * 도움말 문서 목록을 조회한다.
     *
     * <p>비로그인 사용자도 조회할 수 있다.</p>
     *
     * @param category 필터 카테고리 (선택)
     * @return 도움말 목록 (200 OK)
     */
    @Operation(summary = "도움말 목록 조회", description = "카테고리별 또는 전체 도움말 문서를 조회한다 (비인증)")
    @GetMapping("/help")
    public ResponseEntity<List<HelpArticleResponse>> getHelpArticles(
            @Parameter(description = "카테고리 필터 (선택)")
            @RequestParam(required = false) String category
    ) {
        return ResponseEntity.ok(supportService.getHelpArticles(category));
    }

    // ─────────────────────────────────────────────
    // 상담 티켓
    // ─────────────────────────────────────────────

    /**
     * 상담 티켓을 생성한다.
     *
     * <p>로그인한 사용자만 가능하다. 생성 시 상태는 OPEN으로 초기화된다.</p>
     *
     * @param userId  JWT에서 추출한 사용자 ID
     * @param request 티켓 생성 요청 (category, title, content)
     * @return 생성된 티켓 정보 (201 Created)
     */
    @Operation(summary = "상담 티켓 생성", description = "1:1 문의 티켓을 생성한다 (인증 필수)")
    @PostMapping("/tickets")
    public ResponseEntity<TicketResponse> createTicket(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody TicketCreateRequest request
    ) {
        TicketResponse response = supportService.createTicket(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 현재 사용자의 상담 티켓 목록을 페이징으로 조회한다.
     *
     * @param userId JWT에서 추출한 사용자 ID
     * @param page   페이지 번호 (기본 0)
     * @param size   페이지 크기 (기본 10, 최대 50)
     * @return 티켓 페이지 (200 OK)
     */
    @Operation(summary = "내 문의 목록 조회", description = "로그인 사용자의 상담 티켓 목록을 페이징 조회한다 (인증 필수)")
    @GetMapping("/tickets")
    public ResponseEntity<Page<TicketResponse>> getMyTickets(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(supportService.getMyTickets(userId, page, size));
    }
}
