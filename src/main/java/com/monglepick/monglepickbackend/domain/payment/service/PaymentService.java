package com.monglepick.monglepickbackend.domain.payment.service;

import com.monglepick.monglepickbackend.domain.payment.client.TossPaymentsClient;
import com.monglepick.monglepickbackend.domain.payment.dto.PaymentDto.ConfirmRequest;
import com.monglepick.monglepickbackend.domain.payment.dto.PaymentDto.ConfirmResponse;
import com.monglepick.monglepickbackend.domain.payment.dto.PaymentDto.CreateOrderRequest;
import com.monglepick.monglepickbackend.domain.payment.dto.PaymentDto.OrderHistoryResponse;
import com.monglepick.monglepickbackend.domain.payment.dto.PaymentDto.OrderResponse;
import com.monglepick.monglepickbackend.domain.payment.entity.PaymentOrder;
import com.monglepick.monglepickbackend.domain.payment.entity.SubscriptionPlan;
import com.monglepick.monglepickbackend.domain.payment.repository.PaymentOrderRepository;
import com.monglepick.monglepickbackend.domain.payment.repository.SubscriptionPlanRepository;
import com.monglepick.monglepickbackend.domain.reward.dto.PointDto;
import com.monglepick.monglepickbackend.domain.reward.service.PointService;
// Jackson 3.x: com.fasterxml.jackson → tools.jackson 패키지 경로 변경 (Spring Boot 4.x)
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.monglepick.monglepickbackend.global.exception.BusinessException;
import com.monglepick.monglepickbackend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 결제 서비스 — Toss Payments 결제 주문 생성, 승인, 내역 조회 비즈니스 로직.
 *
 * <p>클라이언트(monglepick-client)의 결제 플로우를 처리하는 핵심 서비스이다.
 * 결제 플로우는 다음 3단계로 진행된다:</p>
 *
 * <h3>결제 플로우</h3>
 * <ol>
 *   <li><b>주문 생성</b> ({@link #createOrder}): 클라이언트 요청 → DB에 PENDING 주문 생성 → orderId + clientKey 반환</li>
 *   <li><b>결제창 표시</b>: 클라이언트가 Toss SDK로 결제창 호출 (orderId, clientKey 사용)</li>
 *   <li><b>결제 승인</b> ({@link #confirmPayment}): 클라이언트가 paymentKey 전달 → Toss API 승인 → 포인트 지급</li>
 * </ol>
 *
 * <h3>트랜잭션 전략</h3>
 * <ul>
 *   <li>클래스 레벨: {@code @Transactional(readOnly = true)} — 기본 읽기 전용</li>
 *   <li>변경 메서드: 개별 {@code @Transactional} 오버라이드 — 쓰기 트랜잭션</li>
 *   <li>결제 승인 시 Toss API 호출과 DB 상태 변경이 하나의 트랜잭션에 포함됨</li>
 * </ul>
 *
 * <h3>멱등성 보장</h3>
 * <p>같은 orderId로 중복 승인을 시도하면 {@code DUPLICATE_ORDER} 에러가 반환된다.
 * PENDING이 아닌 주문에 대한 승인 시도를 차단하여 이중 결제를 방지한다.</p>
 *
 * @see TossPaymentsClient Toss Payments REST API 클라이언트
 * @see SubscriptionService 구독 관련 비즈니스 로직
 * @see PointService 포인트 지급 서비스
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    /** 결제 주문 리포지토리 */
    private final PaymentOrderRepository orderRepository;

    /** 구독 상품 리포지토리 (구독 결제 시 plan 조회) */
    private final SubscriptionPlanRepository planRepository;

    /** Toss Payments REST API 클라이언트 (결제 승인/취소) */
    private final TossPaymentsClient tossClient;

    /** 포인트 서비스 (결제 완료 후 포인트 지급) */
    private final PointService pointService;

    /** 구독 서비스 (구독 결제 완료 후 UserSubscription 생성) */
    private final SubscriptionService subscriptionService;

    /**
     * 결제 보상 트랜잭션 서비스.
     *
     * <p>Toss 결제 승인 후 DB 저장 실패 + PG 환불도 실패한 경우,
     * COMPENSATION_FAILED 상태를 <b>독립 트랜잭션(REQUIRES_NEW)</b>으로 저장한다.</p>
     *
     * <p>같은 클래스({@code PaymentService}) 내 메서드로 구현하면 Spring AOP 프록시를
     * 경유하지 않아 {@code @Transactional(REQUIRES_NEW)}가 무시된다. 따라서 별도 Bean으로
     * 분리하여 프록시를 통한 트랜잭션 제어가 동작하도록 한다.</p>
     *
     * @see PaymentCompensationService
     */
    private final PaymentCompensationService compensationService;

    /**
     * Toss Payments 클라이언트 키.
     * 클라이언트가 결제창을 열 때 필요한 키. 시크릿 키와 달리 노출 가능.
     * application.yml의 {@code toss.payments.client-key}에서 주입.
     */
    @Value("${toss.payments.client-key}")
    private String clientKey;

    // ──────────────────────────────────────────────
    // 주문 생성
    // ──────────────────────────────────────────────

    /**
     * 결제 주문을 생성한다 (status=PENDING).
     *
     * <p>UUID로 orderId를 생성하고 DB에 저장한 뒤,
     * orderId + clientKey를 반환한다.
     * 클라이언트는 이 값으로 Toss Payments 결제창을 호출한다.</p>
     *
     * <h4>주문 유형별 처리</h4>
     * <ul>
     *   <li>{@code POINT_PACK}: 요청의 pointsAmount를 그대로 사용</li>
     *   <li>{@code SUBSCRIPTION}: planCode로 구독 상품을 조회하여 plan FK 연결 + pointsPerPeriod 자동 설정</li>
     * </ul>
     *
     * @param request 주문 생성 요청 (userId, orderType, amount, pointsAmount?, planCode?)
     * @return 주문 응답 (orderId, amount, clientKey)
     * @throws BusinessException 구독 주문인데 planCode가 유효하지 않은 경우 (ORDER_NOT_FOUND)
     */
    /**
     * 결제 주문을 생성한다 (status=PENDING).
     *
     * <p>멱등키(idempotencyKey)가 제공된 경우:
     * <ul>
     *   <li>동일 키로 기존 주문이 존재하면 기존 주문 응답을 반환 (중복 생성 방지)</li>
     *   <li>기존 주문의 userId/amount가 다르면 IDEMPOTENCY_KEY_REUSE 에러</li>
     * </ul></p>
     *
     * @param request        주문 생성 요청
     * @param idempotencyKey 멱등키 (Idempotency-Key 헤더, nullable)
     * @return 주문 응답 (orderId, amount, clientKey)
     */
    @Transactional
    public OrderResponse createOrder(String userId, CreateOrderRequest request, String idempotencyKey) {
        log.info("주문 생성 시작: userId={}, orderType={}, amount={}, idempotencyKey={}",
                userId, request.orderType(), request.amount(), idempotencyKey);

        // 0. 멱등키가 있으면 기존 주문 확인
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = orderRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                PaymentOrder existingOrder = existing.get();
                // 동일 멱등키인데 요청 내용이 다르면 에러
                if (!existingOrder.getUserId().equals(userId)
                        || !existingOrder.getAmount().equals(request.amount())) {
                    log.warn("멱등키 재사용 감지: idempotencyKey={}, 기존userId={}, 요청userId={}",
                            idempotencyKey, existingOrder.getUserId(), userId);
                    throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REUSE);
                }
                // 동일 요청 → 기존 주문 응답 반환
                log.info("멱등키로 기존 주문 반환: orderId={}", existingOrder.getPaymentOrderId());
                return new OrderResponse(existingOrder.getPaymentOrderId(), existingOrder.getAmount(), clientKey);
            }
        }

        // 1. UUID로 고유한 주문 ID 생성
        String orderId = UUID.randomUUID().toString();

        // 2. 주문 유형 파싱 (대소문자 무관)
        PaymentOrder.OrderType orderType = PaymentOrder.OrderType.valueOf(
                request.orderType().toUpperCase());

        // 3. PaymentOrder 빌더 구성 — JWT에서 추출한 userId 사용 (BOLA 방지)
        PaymentOrder.PaymentOrderBuilder builder = PaymentOrder.builder()
                .paymentOrderId(orderId)
                .userId(userId)
                .orderType(orderType)
                .amount(request.amount())
                .pointsAmount(request.pointsAmount())
                .idempotencyKey(idempotencyKey != null && !idempotencyKey.isBlank() ? idempotencyKey : null)
                .status(PaymentOrder.OrderStatus.PENDING);

        // 4. 구독인 경우 plan 조회 + 금액 검증 + 연결
        if (orderType == PaymentOrder.OrderType.SUBSCRIPTION && request.planCode() != null) {
            SubscriptionPlan plan = planRepository.findByPlanCode(request.planCode())
                    .orElseThrow(() -> {
                        log.error("구독 상품 조회 실패: planCode={}", request.planCode());
                        return new BusinessException(
                                ErrorCode.ORDER_NOT_FOUND,
                                "구독 상품을 찾을 수 없습니다: " + request.planCode()
                        );
                    });

            // 금액 변조 방지: 요청 금액이 상품 가격과 일치하는지 검증
            if (!plan.getPrice().equals(request.amount())) {
                log.error("결제 금액 변조 감지: planCode={}, 상품가격={}, 요청금액={}",
                        request.planCode(), plan.getPrice(), request.amount());
                throw new BusinessException(
                        ErrorCode.PAYMENT_FAILED,
                        "결제 금액이 상품 가격과 일치하지 않습니다. 상품: " + plan.getPrice() + "원, 요청: " + request.amount() + "원"
                );
            }

            // plan FK 연결 + 지급 포인트를 상품 정보에서 가져옴
            builder.plan(plan).pointsAmount(plan.getPointsPerPeriod());
            log.debug("구독 상품 연결: planCode={}, pointsPerPeriod={}",
                    plan.getPlanCode(), plan.getPointsPerPeriod());
        }

        // 5. DB 저장
        orderRepository.save(builder.build());

        log.info("주문 생성 완료: orderId={}, userId={}, orderType={}, amount={}",
                orderId, userId, orderType, request.amount());

        // 6. orderId + clientKey 반환 (클라이언트가 결제창에 사용)
        return new OrderResponse(orderId, request.amount(), clientKey);
    }

    // ──────────────────────────────────────────────
    // 결제 승인
    // ──────────────────────────────────────────────

    /**
     * 결제 승인 + 포인트 지급을 처리한다.
     *
     * <h3>처리 순서</h3>
     * <ol>
     *   <li>orderId로 주문 조회 및 소유자 검증 (BOLA 방지)</li>
     *   <li>PENDING 상태 확인 (중복 처리 방지)</li>
     *   <li>금액 일치 검증 (위변조 방지)</li>
     *   <li>Toss Payments 결제 승인 API 호출 (외부 I/O)</li>
     *   <li>주문 상태 COMPLETED + 포인트 지급 + 구독 활성화 (DB 원자적 처리)</li>
     * </ol>
     *
     * <h3>트랜잭션 경계 설계</h3>
     * <p>Toss API 호출(step 4)은 {@code @Transactional} 범위 안에 있다.
     * Toss API가 성공한 뒤 DB 처리(step 5)가 실패하면 트랜잭션이 롤백되어
     * 주문 상태가 PENDING으로 복원된다.</p>
     *
     * <p>이 경우 아래 두 단계의 보상(compensation) 처리를 수행한다:</p>
     * <ol>
     *   <li><b>Toss 환불 시도</b>: {@code cancelPayment}를 최대 {@value #MAX_CANCEL_RETRIES}회 재시도</li>
     *   <li><b>COMPENSATION_FAILED 기록</b>: 환불도 실패하면
     *       {@link PaymentCompensationService#recordCompensationFailed}를 통해
     *       <b>독립 트랜잭션(REQUIRES_NEW)</b>으로 상태를 저장.
     *       원본 트랜잭션이 rollback-only 상태여도 별도 트랜잭션이므로 커밋이 보장된다.</li>
     * </ol>
     *
     * <h3>멱등성</h3>
     * <p>PENDING이 아닌 주문에 대한 승인 요청은 {@code DUPLICATE_ORDER}로 거부된다.</p>
     *
     * @param userId  JWT에서 추출한 현재 사용자 ID
     * @param request 결제 승인 요청 (orderId, paymentKey, amount)
     * @return 승인 결과 (success, pointsGranted, newBalance)
     * @throws BusinessException 주문 미발견(ORDER_NOT_FOUND), 중복 처리(DUPLICATE_ORDER),
     *                           금액 불일치(PAYMENT_FAILED), PG 승인 실패(PAYMENT_FAILED)
     */
    @Transactional
    public ConfirmResponse confirmPayment(String userId, ConfirmRequest request) {
        log.info("결제 승인 시작: userId={}, orderId={}, amount={}", userId, request.orderId(), request.amount());

        // ── 1. 주문 조회 ──
        PaymentOrder order = orderRepository.findByPaymentOrderId(request.orderId())
                .orElseThrow(() -> {
                    log.error("주문 조회 실패: orderId={}", request.orderId());
                    return new BusinessException(ErrorCode.ORDER_NOT_FOUND);
                });

        // ── 1-1. 주문 소유자 검증 (BOLA 방지 — 타인의 주문을 승인할 수 없음) ──
        if (!order.getUserId().equals(userId)) {
            log.error("주문 소유자 불일치: orderId={}, 주문소유자={}, 요청자={}",
                    request.orderId(), order.getUserId(), userId);
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        // ── 2. 중복 결제 방지 (멱등성) ──
        // PENDING이 아닌 주문(COMPLETED/FAILED/REFUNDED/COMPENSATION_FAILED)은 재처리 불가
        if (order.getStatus() != PaymentOrder.OrderStatus.PENDING) {
            log.warn("중복 결제 시도 차단: orderId={}, currentStatus={}",
                    request.orderId(), order.getStatus());
            throw new BusinessException(ErrorCode.DUPLICATE_ORDER);
        }

        // ── 3. 금액 일치 검증 (클라이언트 위변조 방지) ──
        // Integer 박싱 타입은 == 대신 equals() 사용 필수 (값 128 초과 시 == 은 false)
        if (!order.getAmount().equals(request.amount())) {
            log.error("결제 금액 불일치: orderId={}, 주문금액={}, 요청금액={}",
                    request.orderId(), order.getAmount(), request.amount());
            throw new BusinessException(
                    ErrorCode.PAYMENT_FAILED,
                    "결제 금액이 일치하지 않습니다. 주문: " + order.getAmount() + "원, 요청: " + request.amount() + "원"
            );
        }

        // ── 4. Toss Payments 결제 승인 API 호출 ──
        // 외부 I/O이므로 실패 시 BusinessException(PAYMENT_FAILED)이 발생하고
        // 트랜잭션 롤백으로 DB 변경 없이 종료된다 (안전).
        tossClient.confirmPayment(request.paymentKey(), request.orderId(), request.amount());

        // ── 5. DB 상태 변경 + 포인트 지급 + 구독 활성화 (원자적 처리) ──
        // [이슈 2 핵심] 이 블록 전체가 하나의 트랜잭션 안에서 실행된다.
        // 포인트 지급 실패 시 order.complete()도 함께 롤백되어 일관성이 보장된다.
        // 포인트 지급 성공 후 구독 생성 실패 시에도 전체 롤백되어 포인트 이중지급이 방지된다.
        try {
            // 5-1. 주문 COMPLETED 처리 (pgTransactionId, pgProvider, completedAt 기록)
            order.complete(request.paymentKey(), "TOSS");
            log.info("주문 상태 변경: orderId={}, PENDING → COMPLETED", order.getPaymentOrderId());

            // 5-2. 포인트 지급
            // earnPoint()는 @Transactional(REQUIRED)이므로 현재 트랜잭션에 합류한다.
            // 실패 시 예외가 전파되어 전체 트랜잭션이 롤백 — order.complete()도 취소됨.
            int pointsToGrant = order.getPointsAmount() != null ? order.getPointsAmount() : 0;
            String description = order.getOrderType() == PaymentOrder.OrderType.SUBSCRIPTION
                    ? "구독 포인트 지급" : "포인트팩 구매";

            PointDto.EarnResponse earnResult = null;
            if (pointsToGrant > 0) {
                earnResult = pointService.earnPoint(
                        order.getUserId(),
                        pointsToGrant,
                        "earn",
                        description,
                        order.getPaymentOrderId()
                );
                log.info("포인트 지급 완료: userId={}, points={}, newBalance={}",
                        order.getUserId(), pointsToGrant,
                        earnResult != null ? earnResult.balanceAfter() : "N/A");
            }

            // 5-3. 구독 결제인 경우 UserSubscription 생성
            // 실패 시 예외 전파 → 트랜잭션 전체 롤백 (포인트 지급 + 주문 완료 모두 취소)
            if (order.getOrderType() == PaymentOrder.OrderType.SUBSCRIPTION && order.getPlan() != null) {
                subscriptionService.createSubscription(order.getUserId(), order.getPlan());
                log.info("구독 활성화: userId={}, plan={}", order.getUserId(), order.getPlan().getPlanCode());
            }

            int newBalance = earnResult != null ? earnResult.balanceAfter() : 0;
            log.info("결제 승인 완료: orderId={}, pointsGranted={}, newBalance={}",
                    order.getPaymentOrderId(), pointsToGrant, newBalance);

            return new ConfirmResponse(true, pointsToGrant, newBalance);

        } catch (Exception e) {
            // ── 보상(Compensation) 처리 ──
            // Toss 결제는 성공했으나 DB 처리가 실패한 상황.
            // 이 catch 블록에 진입하면 현재 트랜잭션은 이미 rollback-only 상태이다.
            // (Spring이 unchecked exception 발생 즉시 트랜잭션을 rollback-only로 마킹함)
            log.error("[C-B3] DB 처리 실패 — Toss 결제 보상 취소 시도: orderId={}, error={}",
                    request.orderId(), e.getMessage(), e);

            // ── 보상 취소 재시도 (최대 MAX_CANCEL_RETRIES회) ──
            boolean cancelSuccess = attemptCancelWithRetry(request.paymentKey(), request.orderId());

            if (!cancelSuccess) {
                // ── CRITICAL: Toss 환불도 실패 → COMPENSATION_FAILED 상태 저장 ──
                //
                // [핵심 수정] 기존 코드의 문제:
                //   order.markCompensationFailed() + orderRepository.save(order)를
                //   현재 catch 블록 안에서 직접 호출하면, 현재 트랜잭션이 rollback-only이므로
                //   save()가 호출되어도 실제로 DB에 커밋되지 않는다.
                //
                // [해결] compensationService.recordCompensationFailed()는
                //   @Transactional(REQUIRES_NEW)가 적용된 별도 Spring Bean 메서드이다.
                //   독립 트랜잭션으로 실행되므로 원본 트랜잭션 롤백과 무관하게 커밋이 보장된다.
                log.error("[CRITICAL][C-B3] Toss 보상 취소 {}회 실패 — COMPENSATION_FAILED 기록 및 수동 조치 필요. " +
                                "orderId={}, paymentKey={}, userId={}, amount={}",
                        MAX_CANCEL_RETRIES,
                        request.orderId(),
                        request.paymentKey(),
                        userId,
                        request.amount());

                // REQUIRES_NEW 독립 트랜잭션으로 상태 저장 (원본 롤백과 무관하게 커밋됨)
                String compensationReason = "보상 취소 " + MAX_CANCEL_RETRIES + "회 실패: "
                        + e.getMessage();
                compensationService.recordCompensationFailed(order, compensationReason);
            }

            // 원본 예외를 그대로 전파하여 클라이언트가 결제 실패를 인지하도록 함
            throw e;
        }
    }

    // ──────────────────────────────────────────────
    // 결제 내역 조회
    // ──────────────────────────────────────────────

    /**
     * 사용자의 결제 주문 내역을 페이징으로 조회한다.
     *
     * <p>모든 상태(PENDING, COMPLETED, FAILED, REFUNDED)의 주문이 포함되며,
     * 생성 시각 기준 최신순으로 정렬된다.
     * 클라이언트의 "결제 내역" 화면에서 사용된다.</p>
     *
     * @param userId   사용자 ID
     * @param pageable 페이징 정보 (page, size)
     * @return 결제 주문 내역 페이지
     */
    public Page<OrderHistoryResponse> getOrderHistory(String userId, Pageable pageable) {
        log.debug("결제 내역 조회: userId={}, page={}, size={}",
                userId, pageable.getPageNumber(), pageable.getPageSize());

        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toHistoryResponse);
    }

    // ──────────────────────────────────────────────
    // 웹훅 처리
    // ──────────────────────────────────────────────

    /**
     * Toss 웹훅 서명을 검증하고 이벤트를 처리한다.
     *
     * <p>서명 검증 실패 시 INVALID_WEBHOOK_SIGNATURE 에러를 던진다.
     * 검증 통과 후 이벤트 로깅을 수행한다 (향후 결제 확인 자동화 확장).</p>
     *
     * @param rawBody   웹훅 요청 Body 원문
     * @param signature TossPayments-Signature 헤더 값
     * @throws BusinessException 서명 검증 실패 시 (INVALID_WEBHOOK_SIGNATURE)
     */
    private static final ObjectMapper WEBHOOK_MAPPER = new ObjectMapper();

    @Transactional
    public void verifyAndProcessWebhook(String rawBody, String signature) {
        /* 1. 서명 검증 */
        if (!tossClient.verifyWebhookSignature(rawBody, signature)) {
            log.error("Toss 웹훅 서명 검증 실패");
            throw new BusinessException(ErrorCode.INVALID_WEBHOOK_SIGNATURE);
        }
        log.info("Toss 웹훅 수신 (서명 검증 통과)");

        /* 2. 이벤트 파싱 및 처리 */
        try {
            JsonNode root = WEBHOOK_MAPPER.readTree(rawBody);
            String eventType = root.path("eventType").asText("");
            JsonNode data = root.path("data");

            switch (eventType) {
                case "PAYMENT_STATUS_CHANGED" -> {
                    String orderId = data.path("orderId").asText(null);
                    String status = data.path("status").asText("");

                    if (orderId == null) {
                        log.warn("웹훅 orderId 누락: eventType={}", eventType);
                        return;
                    }

                    /* 취소/환불 처리 */
                    if ("CANCELED".equals(status) || "PARTIAL_CANCELED".equals(status)) {
                        orderRepository.findByPaymentOrderId(orderId).ifPresent(order -> {
                            if (order.getStatus() == PaymentOrder.OrderStatus.COMPLETED) {
                                order.refund();
                                log.info("웹훅 환불 처리 완료: orderId={}", orderId);
                            }
                        });
                    }
                }
                default -> log.debug("미처리 웹훅 이벤트: eventType={}", eventType);
            }
        } catch (Exception e) {
            /* 웹훅 파싱 실패 시에도 200 반환해야 Toss 재시도를 방지함 — 로그만 기록 */
            log.error("웹훅 이벤트 처리 실패 (파싱 오류): error={}", e.getMessage(), e);
        }
    }

    // ──────────────────────────────────────────────
    // 보상 취소 관련 상수
    // ──────────────────────────────────────────────

    /**
     * Toss 보상 취소 최대 재시도 횟수.
     *
     * <p>DB 처리 실패 후 PG 환불을 시도하는 횟수 상한이다.
     * {@code confirmPayment()} catch 블록과 {@code attemptCancelWithRetry()} 헬퍼에서 사용한다.</p>
     */
    private static final int MAX_CANCEL_RETRIES = 3;

    /**
     * Toss 보상 취소 재시도 대기 간격 (밀리초).
     *
     * <p>각 재시도 사이에 이 간격만큼 대기한다.
     * 네트워크 일시 장애에서 빠르게 회복할 수 있도록 짧게 설정한다.</p>
     */
    private static final long CANCEL_RETRY_INTERVAL_MS = 100L;

    // ──────────────────────────────────────────────
    // private 헬퍼
    // ──────────────────────────────────────────────

    /**
     * Toss 결제 보상 취소를 최대 {@value #MAX_CANCEL_RETRIES}회 재시도한다.
     *
     * <p>DB 처리 실패 후 이미 승인된 Toss 결제를 환불하기 위해 호출한다.
     * 각 시도 실패 시 {@value #CANCEL_RETRY_INTERVAL_MS}ms 대기 후 재시도하며,
     * 인터럽트 발생 시 즉시 루프를 탈출한다.</p>
     *
     * <p>이 메서드는 트랜잭션 컨텍스트를 사용하지 않으며 (외부 API 호출만 수행),
     * 호출자의 트랜잭션 상태에 영향을 주지 않는다.</p>
     *
     * @param paymentKey Toss 결제 키 (환불 대상)
     * @param orderId    주문 UUID (로깅용)
     * @return 환불 성공 여부 (true: 성공, false: 재시도 소진 또는 인터럽트)
     */
    private boolean attemptCancelWithRetry(String paymentKey, String orderId) {
        for (int attempt = 1; attempt <= MAX_CANCEL_RETRIES; attempt++) {
            try {
                tossClient.cancelPayment(paymentKey, "서버 내부 오류로 인한 자동 보상 취소");
                log.info("[C-B3] Toss 보상 취소 성공 (시도 {}/{}): orderId={}",
                        attempt, MAX_CANCEL_RETRIES, orderId);
                return true; // 취소 성공
            } catch (Exception cancelEx) {
                log.warn("[C-B3] Toss 보상 취소 실패 (시도 {}/{}): orderId={}, error={}",
                        attempt, MAX_CANCEL_RETRIES, orderId, cancelEx.getMessage());

                // 마지막 시도가 아니면 다음 재시도 전 대기
                if (attempt < MAX_CANCEL_RETRIES) {
                    try {
                        Thread.sleep(CANCEL_RETRY_INTERVAL_MS);
                    } catch (InterruptedException ie) {
                        // 스레드 인터럽트 상태 복원 후 즉시 루프 탈출
                        Thread.currentThread().interrupt();
                        log.warn("[C-B3] 보상 취소 대기 중 인터럽트 — 재시도 중단: orderId={}", orderId);
                        return false;
                    }
                }
            }
        }
        return false; // 재시도 소진
    }

    /**
     * PaymentOrder 엔티티를 OrderHistoryResponse DTO로 변환한다.
     *
     * @param order 결제 주문 엔티티
     * @return 결제 내역 응답 DTO
     */
    private OrderHistoryResponse toHistoryResponse(PaymentOrder order) {
        return new OrderHistoryResponse(
                order.getPaymentOrderId(),
                order.getOrderType().name(),
                order.getAmount(),
                order.getPointsAmount(),
                order.getStatus().name(),
                order.getPgProvider(),
                order.getCompletedAt(),
                order.getCreatedAt()
        );
    }
}
