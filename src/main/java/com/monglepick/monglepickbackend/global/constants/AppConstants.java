package com.monglepick.monglepickbackend.global.constants;

/**
 * 애플리케이션 전역 상수 클래스.
 *
 * <p>하드코딩된 문자열을 중앙에서 관리하여 일관성과 유지보수성을 확보한다.
 * 필터, 핸들러, 컨트롤러 등에서 공통으로 참조한다.</p>
 */
public final class AppConstants {

    private AppConstants() {
        /* 인스턴스 생성 방지 */
    }

    // ──────────────────────────────────────────────
    // HTTP 헤더 이름
    // ──────────────────────────────────────────────

    /** JWT Bearer 토큰을 전달하는 Authorization 헤더 */
    public static final String HEADER_AUTHORIZATION = "Authorization";

    /** AI Agent → Backend 서비스 간 인증 헤더 */
    public static final String HEADER_SERVICE_KEY = "X-Service-Key";

    /** 결제 주문 중복 방지를 위한 멱등키 헤더 */
    public static final String HEADER_IDEMPOTENCY_KEY = "Idempotency-Key";

    /** Toss Payments 웹훅 서명 검증 헤더 */
    public static final String HEADER_TOSS_SIGNATURE = "TossPayments-Signature";

    // ──────────────────────────────────────────────
    // 인증 관련 상수
    // ──────────────────────────────────────────────

    /** Authorization 헤더의 Bearer 토큰 접두사 */
    public static final String BEARER_PREFIX = "Bearer ";

    /** ServiceKey 인증 시 principal 이름 (PointController.resolveUserId와 일치) */
    public static final String SERVICE_PRINCIPAL = "service";

    // ──────────────────────────────────────────────
    // 미디어 타입
    // ──────────────────────────────────────────────

    /** JSON 응답 Content-Type (UTF-8 명시) */
    public static final String CONTENT_TYPE_JSON = "application/json;charset=UTF-8";

    // ──────────────────────────────────────────────
    // 페이징 기본값
    // ──────────────────────────────────────────────

    /** 페이지 크기 상한 (DoS 방지) */
    public static final int MAX_PAGE_SIZE = 100;

    /** 기본 페이지 크기 */
    public static final int DEFAULT_PAGE_SIZE = 20;
}
