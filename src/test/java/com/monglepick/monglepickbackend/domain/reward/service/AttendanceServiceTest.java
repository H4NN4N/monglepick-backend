package com.monglepick.monglepickbackend.domain.reward.service;

import com.monglepick.monglepickbackend.domain.reward.dto.PointDto.AttendanceResponse;
import com.monglepick.monglepickbackend.domain.reward.dto.PointDto.AttendanceStatusResponse;
import com.monglepick.monglepickbackend.domain.reward.dto.PointDto.EarnResponse;
import com.monglepick.monglepickbackend.domain.reward.entity.UserAttendance;
import com.monglepick.monglepickbackend.domain.reward.repository.UserAttendanceRepository;
import com.monglepick.monglepickbackend.global.exception.BusinessException;
import com.monglepick.monglepickbackend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * AttendanceService 단위 테스트.
 *
 * <p>DB 없이 Mockito로 출석 체크 서비스 로직을 검증한다.
 * LocalDate.now()에 의존하는 테스트는 "오늘" 기준으로 작성한다.</p>
 *
 * <h3>테스트 범위</h3>
 * <ul>
 *   <li>{@link AttendanceService#checkIn} — 출석 체크 (정상/중복/스트릭 보너스)</li>
 *   <li>{@link AttendanceService#getStatus} — 출석 현황 조회 (오늘 출석/어제 출석/연속 끊김)</li>
 * </ul>
 *
 * <h3>보너스 정책 (테스트 기준)</h3>
 * <ul>
 *   <li>streak 1~6: 10P</li>
 *   <li>streak 7~29: 30P</li>
 *   <li>streak 30+: 60P</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AttendanceService 단위 테스트")
class AttendanceServiceTest {

    /* ── Mocks ── */

    /** 출석 리포지토리 목 (DB 접근 차단) */
    @Mock
    private UserAttendanceRepository attendanceRepository;

    /**
     * 포인트 서비스 목.
     *
     * <p>AttendanceService가 출석 보상 지급 시 PointService.earnPoint()를 호출한다.
     * 실제 포인트 DB를 사용하지 않고 Mock으로 응답을 제어한다.</p>
     */
    @Mock
    private PointService pointService;

    /** 테스트 대상 서비스 (Mocks 주입) */
    @InjectMocks
    private AttendanceService attendanceService;

    /* ── 공통 픽스처 ── */

    /** 기본 테스트 사용자 ID */
    private static final String USER_ID = "attend-user-001";

    /**
     * UserAttendance 픽스처 생성 헬퍼.
     *
     * @param checkDate   출석 날짜
     * @param streakCount 연속 출석일
     */
    private UserAttendance buildAttendance(LocalDate checkDate, int streakCount) {
        return UserAttendance.builder()
                .userAttendanceId(1L)
                .userId(USER_ID)
                .checkDate(checkDate)
                .streakCount(streakCount)
                .build();
    }

    /**
     * EarnResponse 픽스처 — earnPoint() 목 반환값.
     *
     * @param balance 지급 후 잔액
     */
    private EarnResponse earnResponse(int balance) {
        return new EarnResponse(balance, "BRONZE");
    }

    // ══════════════════════════════════════════════════
    // checkIn — 출석 체크
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("checkIn — 출석 체크")
    class CheckInTest {

        @Test
        @DisplayName("정상 — 첫 출석 (이전 기록 없음, streak=1, 10P 지급)")
        void checkIn_첫출석() {
            // given: 오늘 출석 기록 없음, 이전 출석 기록도 없음
            LocalDate today = LocalDate.now();
            given(attendanceRepository.findByUserIdAndCheckDate(USER_ID, today))
                    .willReturn(Optional.empty());
            given(attendanceRepository.findTopByUserIdOrderByCheckDateDesc(USER_ID))
                    .willReturn(Optional.empty());

            // 출석 기록 저장 목
            UserAttendance savedAttendance = buildAttendance(today, 1);
            given(attendanceRepository.save(any(UserAttendance.class))).willReturn(savedAttendance);

            // 포인트 지급 목 (10P 기본 출석 보상)
            given(pointService.earnPoint(USER_ID, 10, "earn", "출석 체크 보상 (연속 1일)",
                    "attendance-" + today))
                    .willReturn(earnResponse(110));

            // when
            AttendanceResponse response = attendanceService.checkIn(USER_ID);

            // then
            assertThat(response.checkDate()).isEqualTo(today);
            assertThat(response.streakCount()).isEqualTo(1);
            assertThat(response.earnedPoints()).isEqualTo(10);   // 기본 10P
            assertThat(response.currentBalance()).isEqualTo(110);

            // 저장 + 포인트 지급 호출 확인
            verify(attendanceRepository).save(any(UserAttendance.class));
            verify(pointService).earnPoint(USER_ID, 10, "earn", "출석 체크 보상 (연속 1일)",
                    "attendance-" + today);
        }

        @Test
        @DisplayName("연속 출석 — 어제 출석 기록 있으면 streak가 이어짐 (streak=3, 10P)")
        void checkIn_연속출석_streak유지() {
            // given: 어제(yesterday) 출석 streak=2 → 오늘 streak=3
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);

            given(attendanceRepository.findByUserIdAndCheckDate(USER_ID, today))
                    .willReturn(Optional.empty());
            given(attendanceRepository.findTopByUserIdOrderByCheckDateDesc(USER_ID))
                    .willReturn(Optional.of(buildAttendance(yesterday, 2)));

            UserAttendance savedAttendance = buildAttendance(today, 3);
            given(attendanceRepository.save(any(UserAttendance.class))).willReturn(savedAttendance);
            given(pointService.earnPoint(USER_ID, 10, "earn", "출석 체크 보상 (연속 3일)",
                    "attendance-" + today))
                    .willReturn(earnResponse(130));

            // when
            AttendanceResponse response = attendanceService.checkIn(USER_ID);

            // then
            assertThat(response.streakCount()).isEqualTo(3);
            assertThat(response.earnedPoints()).isEqualTo(10);  // streak 3 → 기본 10P
        }

        @Test
        @DisplayName("7일 연속 보너스 — streak=7 달성 시 30P 지급")
        void checkIn_7일연속_보너스() {
            // given: 어제 streak=6 → 오늘 streak=7 (30P 지급)
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);

            given(attendanceRepository.findByUserIdAndCheckDate(USER_ID, today))
                    .willReturn(Optional.empty());
            given(attendanceRepository.findTopByUserIdOrderByCheckDateDesc(USER_ID))
                    .willReturn(Optional.of(buildAttendance(yesterday, 6)));

            UserAttendance savedAttendance = buildAttendance(today, 7);
            given(attendanceRepository.save(any(UserAttendance.class))).willReturn(savedAttendance);
            // streak=7 → 30P 지급
            given(pointService.earnPoint(USER_ID, 30, "earn", "출석 체크 보상 (연속 7일)",
                    "attendance-" + today))
                    .willReturn(earnResponse(330));

            // when
            AttendanceResponse response = attendanceService.checkIn(USER_ID);

            // then
            assertThat(response.streakCount()).isEqualTo(7);
            assertThat(response.earnedPoints()).isEqualTo(30);  // 7일 보너스: 30P
            assertThat(response.currentBalance()).isEqualTo(330);
        }

        @Test
        @DisplayName("30일 연속 보너스 — streak=30 달성 시 60P 지급")
        void checkIn_30일연속_보너스() {
            // given: 어제 streak=29 → 오늘 streak=30 (60P 지급)
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);

            given(attendanceRepository.findByUserIdAndCheckDate(USER_ID, today))
                    .willReturn(Optional.empty());
            given(attendanceRepository.findTopByUserIdOrderByCheckDateDesc(USER_ID))
                    .willReturn(Optional.of(buildAttendance(yesterday, 29)));

            UserAttendance savedAttendance = buildAttendance(today, 30);
            given(attendanceRepository.save(any(UserAttendance.class))).willReturn(savedAttendance);
            // streak=30 → 60P 지급
            given(pointService.earnPoint(USER_ID, 60, "earn", "출석 체크 보상 (연속 30일)",
                    "attendance-" + today))
                    .willReturn(earnResponse(660));

            // when
            AttendanceResponse response = attendanceService.checkIn(USER_ID);

            // then
            assertThat(response.streakCount()).isEqualTo(30);
            assertThat(response.earnedPoints()).isEqualTo(60);  // 30일 보너스: 60P
        }

        @Test
        @DisplayName("연속 끊김 — 마지막 출석이 어제가 아니면 streak=1로 리셋")
        void checkIn_연속끊김_리셋() {
            // given: 마지막 출석이 3일 전 (streak 끊김 → streak=1로 리셋)
            LocalDate today = LocalDate.now();
            LocalDate threeDaysAgo = today.minusDays(3);

            given(attendanceRepository.findByUserIdAndCheckDate(USER_ID, today))
                    .willReturn(Optional.empty());
            // 마지막 출석이 3일 전 (어제가 아님)
            given(attendanceRepository.findTopByUserIdOrderByCheckDateDesc(USER_ID))
                    .willReturn(Optional.of(buildAttendance(threeDaysAgo, 10)));

            UserAttendance savedAttendance = buildAttendance(today, 1);
            given(attendanceRepository.save(any(UserAttendance.class))).willReturn(savedAttendance);
            given(pointService.earnPoint(USER_ID, 10, "earn", "출석 체크 보상 (연속 1일)",
                    "attendance-" + today))
                    .willReturn(earnResponse(110));

            // when
            AttendanceResponse response = attendanceService.checkIn(USER_ID);

            // then: streak 리셋되어 1, 기본 10P
            assertThat(response.streakCount()).isEqualTo(1);
            assertThat(response.earnedPoints()).isEqualTo(10);
        }

        @Test
        @DisplayName("중복 출석 — 오늘 이미 출석하면 BusinessException(ALREADY_ATTENDED) 발생")
        void checkIn_중복출석_예외() {
            // given: 오늘 이미 출석한 기록이 존재
            LocalDate today = LocalDate.now();
            given(attendanceRepository.findByUserIdAndCheckDate(USER_ID, today))
                    .willReturn(Optional.of(buildAttendance(today, 1)));

            // when / then
            assertThatThrownBy(() -> attendanceService.checkIn(USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.ALREADY_ATTENDED));

            // 저장 및 포인트 지급이 호출되지 않아야 함
            verify(attendanceRepository, never()).save(any());
            verify(pointService, never()).earnPoint(any(), any(int.class), any(), any(), any());
        }
    }

    // ══════════════════════════════════════════════════
    // getStatus — 출석 현황 조회
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("getStatus — 출석 현황 조회")
    class GetStatusTest {

        @Test
        @DisplayName("오늘 출석 완료 — checkedToday=true, currentStreak = 최신 레코드 streak")
        void getStatus_오늘출석완료() {
            // given: 오늘 출석한 기록이 최신
            LocalDate today = LocalDate.now();
            LocalDate monthStart = today.withDayOfMonth(1);
            LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

            given(attendanceRepository.findTopByUserIdOrderByCheckDateDesc(USER_ID))
                    .willReturn(Optional.of(buildAttendance(today, 5)));
            given(attendanceRepository.countByUserId(USER_ID)).willReturn(20L);
            given(attendanceRepository.findByUserIdAndCheckDateBetween(USER_ID, monthStart, monthEnd))
                    .willReturn(List.of(buildAttendance(today, 5)));

            // when
            AttendanceStatusResponse response = attendanceService.getStatus(USER_ID);

            // then
            assertThat(response.checkedToday()).isTrue();
            assertThat(response.currentStreak()).isEqualTo(5);
            assertThat(response.totalDays()).isEqualTo(20);
            assertThat(response.monthlyDates()).hasSize(1).contains(today);
        }

        @Test
        @DisplayName("어제 출석 — checkedToday=false, streak은 유효 (내일 이어갈 수 있음)")
        void getStatus_어제출석() {
            // given: 마지막 출석이 어제
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);
            LocalDate monthStart = today.withDayOfMonth(1);
            LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

            given(attendanceRepository.findTopByUserIdOrderByCheckDateDesc(USER_ID))
                    .willReturn(Optional.of(buildAttendance(yesterday, 3)));
            given(attendanceRepository.countByUserId(USER_ID)).willReturn(3L);
            given(attendanceRepository.findByUserIdAndCheckDateBetween(USER_ID, monthStart, monthEnd))
                    .willReturn(List.of());

            // when
            AttendanceStatusResponse response = attendanceService.getStatus(USER_ID);

            // then: 오늘 출석은 안 했지만 streak은 살아있음
            assertThat(response.checkedToday()).isFalse();
            assertThat(response.currentStreak()).isEqualTo(3);
        }

        @Test
        @DisplayName("연속 끊김 — 마지막 출석이 2일 전이면 streak=0")
        void getStatus_연속끊김() {
            // given: 마지막 출석이 2일 전
            LocalDate today = LocalDate.now();
            LocalDate twoDaysAgo = today.minusDays(2);
            LocalDate monthStart = today.withDayOfMonth(1);
            LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

            given(attendanceRepository.findTopByUserIdOrderByCheckDateDesc(USER_ID))
                    .willReturn(Optional.of(buildAttendance(twoDaysAgo, 7)));
            given(attendanceRepository.countByUserId(USER_ID)).willReturn(7L);
            given(attendanceRepository.findByUserIdAndCheckDateBetween(USER_ID, monthStart, monthEnd))
                    .willReturn(List.of());

            // when
            AttendanceStatusResponse response = attendanceService.getStatus(USER_ID);

            // then: streak 끊김 → 0
            assertThat(response.checkedToday()).isFalse();
            assertThat(response.currentStreak()).isEqualTo(0);
        }

        @Test
        @DisplayName("출석 기록 없음 — streak=0, totalDays=0, checkedToday=false")
        void getStatus_기록없음() {
            // given: 출석 기록 전혀 없는 신규 사용자
            LocalDate today = LocalDate.now();
            LocalDate monthStart = today.withDayOfMonth(1);
            LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

            given(attendanceRepository.findTopByUserIdOrderByCheckDateDesc(USER_ID))
                    .willReturn(Optional.empty());
            given(attendanceRepository.countByUserId(USER_ID)).willReturn(0L);
            given(attendanceRepository.findByUserIdAndCheckDateBetween(USER_ID, monthStart, monthEnd))
                    .willReturn(List.of());

            // when
            AttendanceStatusResponse response = attendanceService.getStatus(USER_ID);

            // then
            assertThat(response.currentStreak()).isEqualTo(0);
            assertThat(response.totalDays()).isEqualTo(0);
            assertThat(response.checkedToday()).isFalse();
            assertThat(response.monthlyDates()).isEmpty();
        }
    }
}
