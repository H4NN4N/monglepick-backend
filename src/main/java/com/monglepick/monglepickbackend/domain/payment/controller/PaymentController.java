package com.monglepick.monglepickbackend.domain.payment.controller;

import com.monglepick.monglepickbackend.domain.payment.dto.PaymentDto;
import com.monglepick.monglepickbackend.domain.payment.dto.PaymentDto.ConfirmRequest;
import com.monglepick.monglepickbackend.domain.payment.dto.PaymentDto.ConfirmResponse;
import com.monglepick.monglepickbackend.domain.payment.dto.PaymentDto.CreateOrderRequest;
import com.monglepick.monglepickbackend.domain.payment.dto.PaymentDto.OrderHistoryResponse;
import com.monglepick.monglepickbackend.domain.payment.dto.PaymentDto.OrderResponse;
import com.monglepick.monglepickbackend.domain.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.monglepick.monglepickbackend.global.exception.BusinessException;
import com.monglepick.monglepickbackend.global.exception.ErrorCode;

import java.security.Principal;

/**
 * 결제 API 컨트롤러 — Toss Payments 결제 주문 생성, 승인, 내역 조회, 웹훅 처리.
 *
 * <p>클라이언트(monglepick-client)가 호출하는 결제 REST API 4개를 제공한다.
 * JWT 인증 기반으로 Principal에서 userId를 추출한다.</p>
 *
 * @see PaymentService 결제 비즈니스 로직
 */
@RestController
@RequestMapping("/api/v1/payment")
@Slf4j
@RequiredArgsConstructor
public class PaymentController {

    /** 결제 서비스 */
    private final PaymentService paymentService;

    // ──────────────────────────────────────────────
    // POST /api/v1/payment/orders — 주문 생성
    // ──────────────────────────────────────────────

    /**
     * 결제 주문을 생성한다.
     *
     * <p>클라이언트가 Toss Payments 결제창을 열기 전에 호출한다.
     * 서버에서 UUID 기반의 orderId를 생성하고, Toss clientKey와 함께 반환한다.</p>
     *
     * <h4>요청 예시</h4>
     * <pre>{@code
     * POST /api/v1/payment/orders
     * Content-Type: application/json
     *
     * {
     *   "userId": "user_abc123",
     *   "orderType": "point_pack",
     *   "amount": 3900,
     *   "pointsAmount": 5000,
     *   "planCode": null
     * }
     * }</pre>
     *
     * <h4>응답 예시</h4>
     * <pre>{@code
     * {
     *   "orderId": "550e8400-e29b-41d4-a716-446655440000",
     *   "amount": 3900,
     *   "clientKey": "test_ck_D5GePWvyJnrK0W0k6q8gLzN97Eoq"
     * }
     * }</pre>
     *
     * @param request 주문 생성 요청 (userId, orderType, amount, pointsAmount?, planCode?)
     * @return 200 OK + OrderResponse (orderId, amount, clientKey)
     */
    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> createOrder(
            Principal principal,
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        // JWT에서 userId를 추출하여 사용 (request body의 userId 무시 — BOLA 방지)
        String userId = resolveUserId(principal);
        log.info("주문 생성 API 호출: userId={}, orderType={}, amount={}, idempotencyKey={}",
                userId, request.orderType(), request.amount(), idempotencyKey);

        OrderResponse response = paymentService.createOrder(userId, request, idempotencyKey);
        return ResponseEntity.ok(response);
    }

    // ──────────────────────────────────────────────
    // POST /api/v1/payment/confirm — 결제 승인
    // ──────────────────────────────────────────────

    /**
     * 결제를 승인하고 포인트를 지급한다.
     *
     * <p>Toss Payments 결제창에서 사용자가 결제를 완료한 후,
     * 클라이언트가 리다이렉트 URL에서 받은 paymentKey와 함께 호출한다.
     * Toss API 승인 → DB 상태 변경 → 포인트 지급이 하나의 트랜잭션으로 처리된다.</p>
     *
     * <h4>요청 예시</h4>
     * <pre>{@code
     * POST /api/v1/payment/confirm
     * Content-Type: application/json
     *
     * {
     *   "orderId": "550e8400-e29b-41d4-a716-446655440000",
     *   "paymentKey": "tgen_20240101abcdef",
     *   "amount": 3900
     * }
     * }</pre>
     *
     * <h4>응답 예시</h4>
     * <pre>{@code
     * {
     *   "success": true,
     *   "pointsGranted": 5000,
     *   "newBalance": 8500
     * }
     * }</pre>
     *
     * @param request 결제 승인 요청 (orderId, paymentKey, amount)
     * @return 200 OK + ConfirmResponse (success, pointsGranted, newBalance)
     */
    @PostMapping("/confirm")
    public ResponseEntity<ConfirmResponse> confirmPayment(
            Principal principal,
            @Valid @RequestBody ConfirmRequest request) {
        // JWT에서 userId 추출 — 주문 소유자 검증에 사용 (BOLA 방지)
        String userId = resolveUserId(principal);
        log.info("결제 승인 API 호출: userId={}, orderId={}, amount={}", userId, request.orderId(), request.amount());

        ConfirmResponse response = paymentService.confirmPayment(userId, request);
        return ResponseEntity.ok(response);
    }

    // ──────────────────────────────────────────────
    // GET /api/v1/payment/orders — 결제 내역 조회
    // ──────────────────────────────────────────────

    /**
     * 사용자의 결제 내역을 페이징으로 조회한다.
     *
     * <p>클라이언트의 "결제 내역" 화면에서 사용한다.
     * 모든 상태(PENDING, COMPLETED, FAILED, REFUNDED)의 주문이 포함되며,
     * 최신순으로 정렬된다.</p>
     *
     * <h4>요청 예시</h4>
     * <pre>{@code
     * GET /api/v1/payment/orders?userId=user_abc123&page=0&size=20
     * }</pre>
     *
     * @param userId 사용자 ID (필수)
     * @param page   페이지 번호 (0부터 시작, 기본값: 0)
     * @param size   페이지 크기 (기본값: 20)
     * @return 200 OK + Page<OrderHistoryResponse>
     */
    @GetMapping("/orders")
    public ResponseEntity<Page<OrderHistoryResponse>> getOrders(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String userId = resolveUserId(principal);
        log.debug("결제 내역 조회 API 호출: userId={}, page={}, size={}", userId, page, size);

        // 페이지 크기 상한 제한 (과도한 요청으로 인한 DB 과부하 방지)
        int safeSize = Math.min(size, 100);
        Page<OrderHistoryResponse> orders = paymentService.getOrderHistory(
                userId, PageRequest.of(page, safeSize));
        return ResponseEntity.ok(orders);
    }

    // ──────────────────────────────────────────────
    // POST /api/v1/payment/webhook — Toss 웹훅
    // ──────────────────────────────────────────────

    /**
     * Toss Payments 웹훅을 수신한다.
     *
     * <p>Toss Payments가 결제 상태 변경 시 이 엔드포인트로 POST 요청을 전송한다.
     * 현재는 로깅용으로만 수신하며, 향후 결제 확인 자동화에 활용할 예정이다.</p>
     *
     * <h4>주요 eventType</h4>
     * <ul>
     *   <li>{@code PAYMENT_STATUS_CHANGED} — 결제 상태 변경 (승인/취소/환불)</li>
     *   <li>{@code PAYOUT_STATUS_CHANGED} — 정산 상태 변경</li>
     * </ul>
     *
     * <h4>보안 고려사항</h4>
     * <p>운영 환경에서는 Toss 웹훅 서명(Webhook Signature) 검증을 추가해야 한다.
     * 현재는 로깅만 수행하므로 서명 검증을 생략한다.</p>
     *
     * @param payload Toss 웹훅 페이로드 (eventType, data)
     * @return 200 OK (Toss가 200 응답을 받아야 웹훅을 재전송하지 않음)
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "TossPayments-Signature", required = false) String signature) {
        // 웹훅 서명 검증 (운영 환경에서 위변조 방지)
        paymentService.verifyAndProcessWebhook(rawBody, signature);
        return ResponseEntity.ok().build();
    }

    // ──────────────────────────────────────────────
    // private 헬퍼
    // ──────────────────────────────────────────────

    /**
     * Principal에서 userId를 안전하게 추출한다.
     * null인 경우 UNAUTHORIZED 예외를 던진다 (NPE 방지).
     */
    private String resolveUserId(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return principal.getName();
    }
}
