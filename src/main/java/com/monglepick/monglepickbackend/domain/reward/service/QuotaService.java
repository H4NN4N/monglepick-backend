package com.monglepick.monglepickbackend.domain.reward.service;

import com.monglepick.monglepickbackend.domain.reward.config.QuotaProperties;
import com.monglepick.monglepickbackend.domain.reward.dto.QuotaDto;
import com.monglepick.monglepickbackend.domain.reward.dto.QuotaDto.GradeQuota;
import com.monglepick.monglepickbackend.domain.reward.dto.QuotaDto.QuotaCheckResult;
import com.monglepick.monglepickbackend.domain.reward.repository.PointsHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 등급별 쿼터(이용 한도) 서비스 — AI 추천 일일/월간 사용 횟수 제한 관리.
 *
 * <p>사용자 등급(BRONZE, SILVER, GOLD, PLATINUM)에 따라 AI 추천 사용 횟수를 제한하고,
 * 등급별 무료 사용 횟수를 관리한다. {@code application.yml}의 {@code app.quota.*} 설정값을
 * 기반으로 동작한다.</p>
 *
 * <h3>쿼터 검사 흐름</h3>
 * <ol>
 *   <li>사용자 등급에 해당하는 {@link GradeQuota} 설정을 조회한다.</li>
 *   <li>{@code points_history} 테이블에서 오늘/이번 달 AI 추천 사용 횟수를 카운트한다.</li>
 *   <li>일일 한도 → 월간 한도 순서로 초과 여부를 검사한다.</li>
 *   <li>한도 내이면 무료 잔여 횟수를 계산하여 실제 차감 포인트(effectiveCost)를 결정한다.</li>
 * </ol>
 *
 * <h3>무제한 처리</h3>
 * <p>{@code dailyLimit} 또는 {@code monthlyLimit}이 -1이면 해당 한도는 무제한으로 간주한다.
 * 현재 PLATINUM 등급이 일일/월간 모두 무제한이다.</p>
 *
 * <h3>사용 횟수 카운트 기준</h3>
 * <p>{@code points_history} 테이블에서 {@code point_type='spend'} 이고
 * {@code description}에 "AI 추천" 키워드가 포함된 레코드를 카운트한다.
 * 무료 사용 시에도 {@code point_change=0}으로 이력이 기록되므로 정확한 카운트가 가능하다.</p>
 *
 * <h3>의존성</h3>
 * <p>이 서비스는 {@link PointsHistoryRepository}만 주입받으므로 {@link PointService}와
 * 순환 참조가 발생하지 않는다. {@link PointService}가 이 서비스를 주입받는 구조이다.</p>
 *
 * @see QuotaDto.QuotaCheckResult
 * @see QuotaDto.GradeQuota
 * @see PointService#checkPoint(String, int)
 */
@Service
@Slf4j
public class QuotaService {

    /** 포인트 변동 이력 리포지토리 — AI 추천 사용 횟수 카운트에 사용 */
    private final PointsHistoryRepository historyRepository;

    /**
     * 등급별 쿼터 설정 맵.
     *
     * <p>키: 등급명(BRONZE, SILVER, GOLD, PLATINUM), 값: 해당 등급의 쿼터 설정.
     * 생성자에서 {@code application.yml}의 {@code app.quota.*} 값으로 초기화된다.
     * 불변 맵(Map.of)으로 생성되어 런타임 변경이 불가하다.</p>
     */
    private final Map<String, GradeQuota> gradeQuotas;

    /**
     * 쿼터 서비스 생성자 — QuotaProperties에서 등급별 설정값을 바인딩받아 초기화한다.
     *
     * <p>기존 16개 @Value 파라미터 대신 {@link QuotaProperties}를 주입받아
     * 생성자 파라미터를 2개로 축소한다.</p>
     *
     * @param historyRepository 포인트 이력 리포지토리 (AI 추천 사용 횟수 카운트)
     * @param props             등급별 쿼터 설정 (@ConfigurationProperties 바인딩)
     */
    public QuotaService(PointsHistoryRepository historyRepository, QuotaProperties props) {
        this.historyRepository = historyRepository;

        // QuotaProperties의 GradeConfig를 GradeQuota DTO로 변환하여 불변 맵 초기화
        this.gradeQuotas = Map.of(
                "BRONZE", toGradeQuota(props.bronze()),
                "SILVER", toGradeQuota(props.silver()),
                "GOLD", toGradeQuota(props.gold()),
                "PLATINUM", toGradeQuota(props.platinum())
        );

        log.info("등급별 쿼터 설정 로드 완료: BRONZE={}, SILVER={}, GOLD={}, PLATINUM={}",
                gradeQuotas.get("BRONZE"), gradeQuotas.get("SILVER"),
                gradeQuotas.get("GOLD"), gradeQuotas.get("PLATINUM"));
    }

    /** QuotaProperties.GradeConfig → GradeQuota DTO 변환 헬퍼 */
    private static GradeQuota toGradeQuota(QuotaProperties.GradeConfig config) {
        return new GradeQuota(config.dailyLimit(), config.monthlyLimit(), config.freeDaily(), config.maxInputLength());
    }

    // ──────────────────────────────────────────────
    // 쿼터 확인 (PointService에서 호출)
    // ──────────────────────────────────────────────

    /**
     * 등급별 AI 추천 쿼터를 확인한다.
     *
     * <p>{@code points_history} 테이블에서 오늘/이번 달 AI 추천 사용 횟수를 카운트하고,
     * 사용자 등급에 해당하는 일일/월간 한도와 비교하여 사용 가능 여부를 판정한다.</p>
     *
     * <h4>검사 순서</h4>
     * <ol>
     *   <li><b>일일 한도 검사</b>: 오늘 사용 횟수 >= 일일 한도이면 즉시 차단
     *       (-1이면 무제한이므로 건너뜀)</li>
     *   <li><b>월간 한도 검사</b>: 이번 달 사용 횟수 >= 월간 한도이면 즉시 차단
     *       (-1이면 무제한이므로 건너뜀)</li>
     *   <li><b>무료 잔여 계산</b>: 오늘 사용 횟수 < 무료 일일 횟수이면 effectiveCost=0</li>
     * </ol>
     *
     * <h4>사용 횟수 카운트 기준</h4>
     * <p>카운트 조건: {@code point_type='spend'} AND {@code description LIKE '%AI 추천%'}.
     * 무료 사용도 이력이 기록되어야 정확한 카운트가 가능하다.</p>
     *
     * @param userId   사용자 ID (VARCHAR(50))
     * @param grade    사용자 등급 문자열 (BRONZE, SILVER, GOLD, PLATINUM).
     *                 null이거나 알 수 없는 등급이면 BRONZE로 fallback
     * @param baseCost 기본 차감 포인트 (포인트가 충분하고 무료 잔여가 없을 때 차감할 금액)
     * @return 쿼터 확인 결과 (사용 가능 여부, 사용 횟수, 한도, 무료 잔여, 실제 비용 등)
     */
    public QuotaCheckResult checkQuota(String userId, String grade, int baseCost) {
        log.debug("쿼터 확인 시작: userId={}, grade={}, baseCost={}", userId, grade, baseCost);

        // 1. 등급별 쿼터 설정 조회 (알 수 없는 등급이면 BRONZE fallback)
        GradeQuota quota = gradeQuotas.getOrDefault(
                grade != null ? grade.toUpperCase() : "BRONZE",
                gradeQuotas.get("BRONZE")
        );

        // 2. 날짜 범위 계산 (오늘 시작~내일 시작, 이번 달 1일 시작)
        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();

        // 3. 일일+월간 AI 추천 사용 횟수를 단일 쿼리로 집계 (DB 부하 절감)
        Object[] counts = historyRepository.countDailyAndMonthlyUsage(
                userId, "spend", "AI 추천", dayStart, monthStart, dayEnd);
        long dailyUsed = counts[0] != null ? ((Number) counts[0]).longValue() : 0;
        long monthlyUsed = counts[1] != null ? ((Number) counts[1]).longValue() : 0;

        log.debug("사용 횟수 조회: userId={}, dailyUsed={}, monthlyUsed={}, quota={}",
                userId, dailyUsed, monthlyUsed, quota);

        // 5. 일일 한도 초과 검사 (-1은 무제한이므로 건너뜀)
        if (quota.dailyLimit() != -1 && dailyUsed >= quota.dailyLimit()) {
            log.info("일일 쿼터 초과: userId={}, dailyUsed={}, dailyLimit={}",
                    userId, dailyUsed, quota.dailyLimit());
            return new QuotaCheckResult(
                    false,
                    (int) dailyUsed,
                    quota.dailyLimit(),
                    (int) monthlyUsed,
                    quota.monthlyLimit(),
                    0,
                    baseCost,
                    quota.maxInputLength(),
                    "일일 AI 추천 한도(" + quota.dailyLimit() + "회)를 초과했습니다."
            );
        }

        // 6. 월간 한도 초과 검사 (-1은 무제한이므로 건너뜀)
        if (quota.monthlyLimit() != -1 && monthlyUsed >= quota.monthlyLimit()) {
            log.info("월간 쿼터 초과: userId={}, monthlyUsed={}, monthlyLimit={}",
                    userId, monthlyUsed, quota.monthlyLimit());
            return new QuotaCheckResult(
                    false,
                    (int) dailyUsed,
                    quota.dailyLimit(),
                    (int) monthlyUsed,
                    quota.monthlyLimit(),
                    0,
                    baseCost,
                    quota.maxInputLength(),
                    "월간 AI 추천 한도(" + quota.monthlyLimit() + "회)를 초과했습니다."
            );
        }

        // 7. 무료 잔여 횟수 계산
        //    오늘 사용 횟수가 무료 일일 횟수 미만이면 무료로 사용 가능
        int freeRemaining = Math.max(0, quota.freeDaily() - (int) dailyUsed);
        int effectiveCost = freeRemaining > 0 ? 0 : baseCost;

        log.debug("쿼터 확인 완료: userId={}, allowed=true, freeRemaining={}, effectiveCost={}",
                userId, freeRemaining, effectiveCost);

        // 8. 쿼터 내 사용 가능 — 정상 응답 반환
        return new QuotaCheckResult(
                true,
                (int) dailyUsed,
                quota.dailyLimit(),
                (int) monthlyUsed,
                quota.monthlyLimit(),
                freeRemaining,
                effectiveCost,
                quota.maxInputLength(),
                ""
        );
    }

    // ──────────────────────────────────────────────
    // 등급별 최대 입력 길이 조회
    // ──────────────────────────────────────────────

    /**
     * 등급별 최대 입력 글자 수를 반환한다.
     *
     * <p>AI Agent가 사용자 입력 길이를 제한할 때 사용한다.
     * 높은 등급일수록 더 긴 메시지를 입력할 수 있다.</p>
     *
     * @param grade 사용자 등급 (BRONZE, SILVER, GOLD, PLATINUM).
     *              null이거나 알 수 없는 등급이면 BRONZE 기본값(200) 반환
     * @return 최대 입력 글자 수
     */
    public int getMaxInputLength(String grade) {
        return gradeQuotas.getOrDefault(
                grade != null ? grade.toUpperCase() : "BRONZE",
                gradeQuotas.get("BRONZE")
        ).maxInputLength();
    }
}
