package com.monglepick.monglepickbackend.domain.reward.service;

import com.monglepick.monglepickbackend.domain.reward.dto.PointDto.BalanceResponse;
import com.monglepick.monglepickbackend.domain.reward.dto.PointDto.CheckResponse;
import com.monglepick.monglepickbackend.domain.reward.dto.PointDto.DeductResponse;
import com.monglepick.monglepickbackend.domain.reward.dto.PointDto.EarnResponse;
import com.monglepick.monglepickbackend.domain.reward.dto.QuotaDto.QuotaCheckResult;
import com.monglepick.monglepickbackend.domain.reward.entity.PointsHistory;
import com.monglepick.monglepickbackend.domain.reward.entity.UserPoint;
import com.monglepick.monglepickbackend.domain.reward.repository.PointsHistoryRepository;
import com.monglepick.monglepickbackend.domain.reward.repository.UserPointRepository;
import com.monglepick.monglepickbackend.global.constants.UserGrade;
import com.monglepick.monglepickbackend.global.exception.BusinessException;
import com.monglepick.monglepickbackend.global.exception.ErrorCode;
import com.monglepick.monglepickbackend.global.exception.InsufficientPointException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * PointService 단위 테스트.
 *
 * <p>DB 없이 Mockito만으로 서비스 레이어 로직을 검증한다.
 * {@code @SpringBootTest}를 사용하지 않으므로 컨텍스트 로딩 없이 빠르게 실행된다.</p>
 *
 * <h3>테스트 범위</h3>
 * <ul>
 *   <li>{@link PointService#checkPoint} — 잔액+쿼터 확인 (정상/레코드 없음/쿼터 초과)</li>
 *   <li>{@link PointService#deductPoint} — 포인트 차감 (정상/잔액 부족/레코드 없음)</li>
 *   <li>{@link PointService#earnPoint} — 포인트 획득 (정상/등급 업그레이드)</li>
 *   <li>{@link PointService#getBalance} — 잔액 조회 (정상/레코드 없음)</li>
 *   <li>{@link PointService#validateUserId} — 입력 검증 (null/빈 문자열)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PointService 단위 테스트")
class PointServiceTest {

    /* ── Mocks ── */

    /** 사용자 포인트 리포지토리 목 (DB 접근 차단) */
    @Mock
    private UserPointRepository userPointRepository;

    /** 포인트 이력 리포지토리 목 (DB 접근 차단) */
    @Mock
    private PointsHistoryRepository pointsHistoryRepository;

    /** 쿼터 서비스 목 (외부 설정 파일 의존성 차단) */
    @Mock
    private QuotaService quotaService;

    /** 테스트 대상 서비스 (Mocks 주입) */
    @InjectMocks
    private PointService pointService;

    /* ── 공통 테스트 픽스처 ── */

    /** 기본 테스트 사용자 ID */
    private static final String USER_ID = "test-user-001";

    /**
     * 기본 쿼터 확인 결과 픽스처 — 사용 가능(allowed=true), 무료 잔여 없음, 비용 그대로 반환.
     *
     * @param cost 테스트에서 사용할 비용
     * @return 정상 쿼터 결과 (allowed=true, effectiveCost=cost)
     */
    private QuotaCheckResult allowedQuota(int cost) {
        // (allowed, dailyUsed, dailyLimit, monthlyUsed, monthlyLimit, freeRemaining, effectiveCost, maxInputLength, message)
        return new QuotaCheckResult(true, 1, 10, 5, 200, 0, cost, 200, "");
    }

    /**
     * 쿼터 초과 결과 픽스처 — 일일 한도 초과 상황.
     */
    private QuotaCheckResult quotaExceeded() {
        return new QuotaCheckResult(false, 10, 10, 50, 200, 0, 100, 200, "일일 AI 추천 한도(10회)를 초과했습니다.");
    }

    /**
     * 무료 사용 가능 쿼터 결과 픽스처 — effectiveCost=0 (무료).
     */
    private QuotaCheckResult freeQuota() {
        return new QuotaCheckResult(true, 0, 10, 0, 200, 2, 0, 200, "");
    }

    /**
     * UserPoint 엔티티 픽스처 생성 헬퍼.
     *
     * @param balance  현재 보유 포인트
     * @param total    누적 획득 포인트
     * @param grade    사용자 등급
     */
    private UserPoint buildUserPoint(int balance, int total, UserGrade grade) {
        return UserPoint.builder()
                .userPointId(1L)
                .userId(USER_ID)
                .pointHave(balance)
                .totalEarned(total)
                .dailyEarned(0)
                .dailyReset(LocalDate.now())
                .userGrade(grade)
                .build();
    }

    // ══════════════════════════════════════════════════
    // checkPoint — 잔액 + 쿼터 통합 확인
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("checkPoint — 잔액 + 쿼터 통합 확인")
    class CheckPointTest {

        @Test
        @DisplayName("정상 — 잔액 충분하고 쿼터 범위 내이면 allowed=true 반환")
        void checkPoint_정상_허용() {
            // given
            int cost = 100;
            UserPoint userPoint = buildUserPoint(500, 500, UserGrade.BRONZE);
            given(userPointRepository.findByUserId(USER_ID)).willReturn(Optional.of(userPoint));
            given(quotaService.checkQuota(USER_ID, "BRONZE", cost)).willReturn(allowedQuota(cost));

            // when
            CheckResponse response = pointService.checkPoint(USER_ID, cost);

            // then
            assertThat(response.allowed()).isTrue();
            assertThat(response.balance()).isEqualTo(500);
            assertThat(response.cost()).isEqualTo(cost);
            assertThat(response.effectiveCost()).isEqualTo(cost);
        }

        @Test
        @DisplayName("잔액 부족 — 잔액이 effectiveCost보다 적으면 allowed=false")
        void checkPoint_잔액부족() {
            // given
            int cost = 100;
            UserPoint userPoint = buildUserPoint(50, 50, UserGrade.BRONZE);
            given(userPointRepository.findByUserId(USER_ID)).willReturn(Optional.of(userPoint));
            given(quotaService.checkQuota(USER_ID, "BRONZE", cost)).willReturn(allowedQuota(cost));

            // when
            CheckResponse response = pointService.checkPoint(USER_ID, cost);

            // then
            assertThat(response.allowed()).isFalse();
            assertThat(response.balance()).isEqualTo(50);
            assertThat(response.message()).contains("포인트가 부족합니다");
        }

        @Test
        @DisplayName("포인트 레코드 없음 — cost>0이면 allowed=false 반환 (예외 미발생)")
        void checkPoint_레코드없음_cost양수() {
            // given: 포인트 레코드 자체가 없는 신규 사용자
            int cost = 100;
            given(userPointRepository.findByUserId(USER_ID)).willReturn(Optional.empty());
            // 레코드 없을 때도 BRONZE 기본값으로 quotaService.checkQuota 호출됨
            given(quotaService.checkQuota(USER_ID, "BRONZE", cost)).willReturn(allowedQuota(cost));

            // when
            CheckResponse response = pointService.checkPoint(USER_ID, cost);

            // then: 예외를 던지지 않고 allowed=false로 응답
            assertThat(response.allowed()).isFalse();
            assertThat(response.balance()).isEqualTo(0);
            assertThat(response.message()).contains("포인트 정보가 없습니다");
        }

        @Test
        @DisplayName("포인트 레코드 없음 — cost=0이면 allowed=true (쿼터 조회만)")
        void checkPoint_레코드없음_cost영() {
            // given
            given(userPointRepository.findByUserId(USER_ID)).willReturn(Optional.empty());
            given(quotaService.checkQuota(USER_ID, "BRONZE", 0)).willReturn(allowedQuota(0));

            // when
            CheckResponse response = pointService.checkPoint(USER_ID, 0);

            // then
            assertThat(response.allowed()).isTrue();
        }

        @Test
        @DisplayName("쿼터 초과 — QuotaService가 allowed=false이면 잔액 무관하게 차단")
        void checkPoint_쿼터초과() {
            // given: 잔액은 충분하지만 일일 쿼터 초과
            UserPoint userPoint = buildUserPoint(1000, 1000, UserGrade.BRONZE);
            given(userPointRepository.findByUserId(USER_ID)).willReturn(Optional.of(userPoint));
            given(quotaService.checkQuota(USER_ID, "BRONZE", 100)).willReturn(quotaExceeded());

            // when
            CheckResponse response = pointService.checkPoint(USER_ID, 100);

            // then: 잔액이 충분해도 쿼터 초과로 차단
            assertThat(response.allowed()).isFalse();
            assertThat(response.message()).contains("일일 AI 추천 한도");
        }

        @Test
        @DisplayName("무료 사용 — freeRemaining>0이면 effectiveCost=0, allowed=true")
        void checkPoint_무료사용() {
            // given
            UserPoint userPoint = buildUserPoint(0, 0, UserGrade.SILVER);
            given(userPointRepository.findByUserId(USER_ID)).willReturn(Optional.of(userPoint));
            // SILVER 등급의 무료 횟수 남아있는 경우
            given(quotaService.checkQuota(USER_ID, "SILVER", 100)).willReturn(freeQuota());

            // when
            CheckResponse response = pointService.checkPoint(USER_ID, 100);

            // then: 잔액이 0이어도 무료 사용이므로 allowed=true
            assertThat(response.allowed()).isTrue();
            assertThat(response.effectiveCost()).isEqualTo(0);
            assertThat(response.message()).contains("무료 AI 추천");
        }

        @Test
        @DisplayName("validateUserId — userId가 null이면 BusinessException(INVALID_INPUT) 발생")
        void checkPoint_userId_null_예외() {
            // given: null userId
            // when / then
            assertThatThrownBy(() -> pointService.checkPoint(null, 100))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_INPUT));
        }

        @Test
        @DisplayName("validateUserId — userId가 공백이면 BusinessException(INVALID_INPUT) 발생")
        void checkPoint_userId_공백_예외() {
            // given: 공백만 있는 userId
            // when / then
            assertThatThrownBy(() -> pointService.checkPoint("   ", 100))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_INPUT));
        }

        @Test
        @DisplayName("validateUserId — userId가 빈 문자열이면 BusinessException(INVALID_INPUT) 발생")
        void checkPoint_userId_빈문자열_예외() {
            // given: 빈 문자열 userId
            // when / then
            assertThatThrownBy(() -> pointService.checkPoint("", 100))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_INPUT));
        }
    }

    // ══════════════════════════════════════════════════
    // deductPoint — 포인트 차감
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("deductPoint — 포인트 차감")
    class DeductPointTest {

        @Test
        @DisplayName("정상 — 잔액 충분하면 차감 후 DeductResponse(success=true) 반환")
        void deductPoint_정상() {
            // given
            int amount = 100;
            UserPoint userPoint = buildUserPoint(500, 500, UserGrade.BRONZE);

            // findByUserIdForUpdate: 비관적 락 쿼리 목
            given(userPointRepository.findByUserIdForUpdate(USER_ID)).willReturn(Optional.of(userPoint));

            // PointsHistory.save() 호출 시 historyId=99L 반환하도록 설정
            PointsHistory savedHistory = PointsHistory.builder()
                    .pointsHistoryId(99L)
                    .userId(USER_ID)
                    .pointChange(-amount)
                    .pointAfter(400)
                    .pointType("spend")
                    .description("테스트 차감")
                    .build();
            given(pointsHistoryRepository.save(any(PointsHistory.class))).willReturn(savedHistory);

            // when
            DeductResponse response = pointService.deductPoint(USER_ID, amount, "session-1", "테스트 차감");

            // then
            assertThat(response.success()).isTrue();
            assertThat(response.balanceAfter()).isEqualTo(400);  // 500 - 100
            assertThat(response.transactionId()).isEqualTo(99L);

            // 이력이 저장됐는지 확인
            verify(pointsHistoryRepository).save(any(PointsHistory.class));
        }

        @Test
        @DisplayName("잔액 부족 — InsufficientPointException(P001) 발생")
        void deductPoint_잔액부족() {
            // given: 잔액 50, 필요 100
            int amount = 100;
            UserPoint userPoint = buildUserPoint(50, 50, UserGrade.BRONZE);
            given(userPointRepository.findByUserIdForUpdate(USER_ID)).willReturn(Optional.of(userPoint));

            // when / then
            assertThatThrownBy(() -> pointService.deductPoint(USER_ID, amount, null, null))
                    .isInstanceOf(InsufficientPointException.class)
                    .satisfies(ex -> {
                        InsufficientPointException ipex = (InsufficientPointException) ex;
                        assertThat(ipex.getBalance()).isEqualTo(50);
                        assertThat(ipex.getRequired()).isEqualTo(100);
                        assertThat(ipex.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_POINT);
                    });

            // 이력이 저장되지 않아야 함
            verify(pointsHistoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("포인트 레코드 없음 — BusinessException(POINT_NOT_FOUND) 발생")
        void deductPoint_레코드없음() {
            // given: findByUserIdForUpdate가 empty 반환 (레코드 없음)
            given(userPointRepository.findByUserIdForUpdate(USER_ID)).willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> pointService.deductPoint(USER_ID, 100, null, null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.POINT_NOT_FOUND));
        }

        @Test
        @DisplayName("동시성 시나리오 — 잔액 정확히 0이 되는 전액 차감 성공")
        void deductPoint_전액차감() {
            // given: 보유 포인트와 요청 금액이 정확히 일치
            int amount = 500;
            UserPoint userPoint = buildUserPoint(500, 1000, UserGrade.SILVER);
            given(userPointRepository.findByUserIdForUpdate(USER_ID)).willReturn(Optional.of(userPoint));

            PointsHistory savedHistory = PointsHistory.builder()
                    .pointsHistoryId(200L)
                    .userId(USER_ID)
                    .pointChange(-500)
                    .pointAfter(0)
                    .pointType("spend")
                    .build();
            given(pointsHistoryRepository.save(any(PointsHistory.class))).willReturn(savedHistory);

            // when
            DeductResponse response = pointService.deductPoint(USER_ID, amount, null, null);

            // then: 잔액 0 — 부족이 아니라 정확히 0 도달
            assertThat(response.success()).isTrue();
            assertThat(response.balanceAfter()).isEqualTo(0);
        }

        @Test
        @DisplayName("validateUserId — userId null이면 BusinessException(INVALID_INPUT)")
        void deductPoint_userId_null() {
            assertThatThrownBy(() -> pointService.deductPoint(null, 100, null, null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_INPUT));

            // findByUserIdForUpdate가 호출되지 않아야 함
            verify(userPointRepository, never()).findByUserIdForUpdate(anyString());
        }
    }

    // ══════════════════════════════════════════════════
    // earnPoint — 포인트 획득
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("earnPoint — 포인트 획득")
    class EarnPointTest {

        @Test
        @DisplayName("정상 — 포인트 획득 후 EarnResponse 반환 (잔액 + 등급 포함)")
        void earnPoint_정상() {
            // given: 잔액 900, 누적 900 → 획득 후 1000 → SILVER 등급 달성
            int amount = 100;
            UserPoint userPoint = buildUserPoint(900, 900, UserGrade.BRONZE);
            given(userPointRepository.findByUserIdForUpdate(USER_ID)).willReturn(Optional.of(userPoint));

            PointsHistory savedHistory = PointsHistory.builder()
                    .pointsHistoryId(300L)
                    .userId(USER_ID)
                    .pointChange(amount)
                    .pointAfter(1000)
                    .pointType("earn")
                    .build();
            given(pointsHistoryRepository.save(any(PointsHistory.class))).willReturn(savedHistory);

            // when
            EarnResponse response = pointService.earnPoint(USER_ID, amount, "earn", "출석 보상", null);

            // then: 잔액 1000, 등급 SILVER (누적 1000 달성)
            assertThat(response.balanceAfter()).isEqualTo(1000);
            assertThat(response.grade()).isEqualTo("SILVER");

            // 이력 저장 확인
            verify(pointsHistoryRepository).save(any(PointsHistory.class));
        }

        @Test
        @DisplayName("포인트 레코드 없음 — BusinessException(POINT_NOT_FOUND) 발생")
        void earnPoint_레코드없음() {
            // given
            given(userPointRepository.findByUserIdForUpdate(USER_ID)).willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> pointService.earnPoint(USER_ID, 50, "earn", null, null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.POINT_NOT_FOUND));
        }

        @Test
        @DisplayName("등급 변경 없음 — BRONZE 유지 시 등급 그대로 반환")
        void earnPoint_등급유지() {
            // given: 누적 100 → 200 (BRONZE 유지)
            UserPoint userPoint = buildUserPoint(100, 100, UserGrade.BRONZE);
            given(userPointRepository.findByUserIdForUpdate(USER_ID)).willReturn(Optional.of(userPoint));

            PointsHistory savedHistory = PointsHistory.builder()
                    .pointsHistoryId(301L)
                    .userId(USER_ID)
                    .pointChange(100)
                    .pointAfter(200)
                    .pointType("earn")
                    .build();
            given(pointsHistoryRepository.save(any(PointsHistory.class))).willReturn(savedHistory);

            // when
            EarnResponse response = pointService.earnPoint(USER_ID, 100, "earn", null, null);

            // then: 누적 200이므로 BRONZE 유지
            assertThat(response.grade()).isEqualTo("BRONZE");
        }

        @Test
        @DisplayName("등급 GOLD 달성 — 누적 5000 이상이면 GOLD 반환")
        void earnPoint_GOLD_달성() {
            // given: 누적 4900 → 100 추가 → 5000 → GOLD
            UserPoint userPoint = buildUserPoint(4900, 4900, UserGrade.SILVER);
            given(userPointRepository.findByUserIdForUpdate(USER_ID)).willReturn(Optional.of(userPoint));

            PointsHistory savedHistory = PointsHistory.builder()
                    .pointsHistoryId(302L)
                    .userId(USER_ID)
                    .pointChange(100)
                    .pointAfter(5000)
                    .pointType("earn")
                    .build();
            given(pointsHistoryRepository.save(any(PointsHistory.class))).willReturn(savedHistory);

            // when
            EarnResponse response = pointService.earnPoint(USER_ID, 100, "earn", "이벤트 보상", null);

            // then
            assertThat(response.grade()).isEqualTo("GOLD");
        }
    }

    // ══════════════════════════════════════════════════
    // getBalance — 잔액 조회
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("getBalance — 잔액 조회")
    class GetBalanceTest {

        @Test
        @DisplayName("정상 — 포인트 레코드 존재 시 잔액/등급/누적 반환")
        void getBalance_정상() {
            // given
            UserPoint userPoint = buildUserPoint(1500, 2000, UserGrade.SILVER);
            given(userPointRepository.findByUserId(USER_ID)).willReturn(Optional.of(userPoint));

            // when
            BalanceResponse response = pointService.getBalance(USER_ID);

            // then
            assertThat(response.balance()).isEqualTo(1500);
            assertThat(response.grade()).isEqualTo("SILVER");
            assertThat(response.totalEarned()).isEqualTo(2000);
        }

        @Test
        @DisplayName("레코드 없음 — 기본값(잔액 0, BRONZE, 누적 0) 반환 (예외 미발생)")
        void getBalance_레코드없음_기본값() {
            // given
            given(userPointRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

            // when
            BalanceResponse response = pointService.getBalance(USER_ID);

            // then: 예외 대신 기본값 반환
            assertThat(response.balance()).isEqualTo(0);
            assertThat(response.grade()).isEqualTo("BRONZE");
            assertThat(response.totalEarned()).isEqualTo(0);
        }

        @Test
        @DisplayName("userGrade가 null인 레코드 — BRONZE로 fallback")
        void getBalance_grade_null_fallback() {
            // given: 구버전 데이터 등 userGrade가 null인 경우
            UserPoint userPoint = UserPoint.builder()
                    .userPointId(1L)
                    .userId(USER_ID)
                    .pointHave(300)
                    .totalEarned(300)
                    .userGrade(null)  // null 등급
                    .build();
            given(userPointRepository.findByUserId(USER_ID)).willReturn(Optional.of(userPoint));

            // when
            BalanceResponse response = pointService.getBalance(USER_ID);

            // then: null → BRONZE fallback
            assertThat(response.grade()).isEqualTo("BRONZE");
        }
    }
}
