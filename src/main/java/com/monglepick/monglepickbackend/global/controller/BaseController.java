package com.monglepick.monglepickbackend.global.controller;

import com.monglepick.monglepickbackend.global.constants.AppConstants;
import com.monglepick.monglepickbackend.global.exception.BusinessException;
import com.monglepick.monglepickbackend.global.exception.ErrorCode;

import java.security.Principal;

/**
 * 컨트롤러 공통 기능 추상 클래스.
 *
 * <p>JWT/ServiceKey 인증에서 userId 추출, 페이지 크기 제한 등
 * 여러 컨트롤러에서 중복되는 로직을 중앙에서 관리한다.</p>
 *
 * <h3>사용법</h3>
 * <pre>{@code
 * @RestController
 * public class MyController extends BaseController {
 *     public ResponseEntity<?> myEndpoint(Principal principal) {
 *         String userId = resolveUserId(principal);
 *         // ...
 *     }
 * }
 * }</pre>
 *
 * @see com.monglepick.monglepickbackend.domain.reward.controller.PointController
 * @see com.monglepick.monglepickbackend.domain.payment.controller.PaymentController
 * @see com.monglepick.monglepickbackend.domain.payment.controller.SubscriptionController
 */
public abstract class BaseController {

    /**
     * Principal에서 userId를 안전하게 추출한다 (JWT 전용).
     *
     * <p>null이거나 getName()이 null이면 UNAUTHORIZED 예외를 던진다.</p>
     *
     * @param principal 인증된 사용자 정보
     * @return 사용자 ID
     * @throws BusinessException 인증 정보가 없는 경우
     */
    protected String resolveUserId(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return principal.getName();
    }

    /**
     * Principal에서 userId를 추출한다 (ServiceKey + JWT 혼합 인증).
     *
     * <p>ServiceKey 인증인 경우 요청 파라미터의 userId를 사용하고,
     * JWT 인증인 경우 토큰에서 추출된 userId를 사용한다.</p>
     *
     * @param principal     인증된 사용자 정보
     * @param requestUserId 요청에 포함된 userId (Agent 호출 시 사용, nullable)
     * @return 확인된 사용자 ID
     * @throws BusinessException 인증 정보가 없거나, ServiceKey인데 userId 누락
     */
    protected String resolveUserIdWithServiceKey(Principal principal, String requestUserId) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        String principalName = principal.getName();

        // ServiceKey 인증: Agent가 요청 파라미터로 userId를 전달
        if (AppConstants.SERVICE_PRINCIPAL.equals(principalName)) {
            if (requestUserId == null || requestUserId.isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "서비스 호출 시 userId는 필수입니다");
            }
            return requestUserId;
        }

        // JWT 인증: 토큰에서 추출한 userId 사용
        return principalName;
    }

    /**
     * 페이지 크기를 상한값으로 제한한다 (대량 조회 DoS 방지).
     *
     * @param size 요청된 페이지 크기
     * @return 제한된 페이지 크기 (최대 {@link AppConstants#MAX_PAGE_SIZE})
     */
    protected int limitPageSize(int size) {
        return Math.min(size, AppConstants.MAX_PAGE_SIZE);
    }
}
