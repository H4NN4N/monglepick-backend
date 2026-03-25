package com.monglepick.monglepickbackend.global.constants;

/**
 * 사용자 등급 열거형.
 *
 * <p>누적 획득 포인트(totalEarned)에 따라 결정되며,
 * 포인트를 사용해도 등급이 하락하지 않는다.</p>
 *
 * <h3>등급별 기준</h3>
 * <ul>
 *   <li>BRONZE: 0~999 (기본 등급)</li>
 *   <li>SILVER: 1,000~4,999</li>
 *   <li>GOLD: 5,000~19,999</li>
 *   <li>PLATINUM: 20,000+</li>
 * </ul>
 *
 * @see com.monglepick.monglepickbackend.domain.reward.entity.UserPoint
 * @see com.monglepick.monglepickbackend.domain.reward.service.PointService
 */
public enum UserGrade {

    /** 기본 등급 (누적 0~999 포인트) */
    BRONZE(0),

    /** 실버 등급 (누적 1,000~4,999 포인트) */
    SILVER(1_000),

    /** 골드 등급 (누적 5,000~19,999 포인트) */
    GOLD(5_000),

    /** 플래티넘 등급 (누적 20,000+ 포인트) */
    PLATINUM(20_000);

    /** 이 등급에 도달하기 위한 최소 누적 포인트 */
    private final int threshold;

    UserGrade(int threshold) {
        this.threshold = threshold;
    }

    public int getThreshold() {
        return threshold;
    }

    /**
     * 누적 포인트 기반으로 등급을 계산한다.
     *
     * @param totalEarned 누적 획득 포인트
     * @return 해당하는 등급
     */
    public static UserGrade fromTotalEarned(int totalEarned) {
        if (totalEarned >= PLATINUM.threshold) return PLATINUM;
        if (totalEarned >= GOLD.threshold) return GOLD;
        if (totalEarned >= SILVER.threshold) return SILVER;
        return BRONZE;
    }

    /**
     * 문자열로부터 등급을 안전하게 파싱한다.
     * null이거나 알 수 없는 값이면 BRONZE를 반환한다.
     *
     * @param value 등급 문자열 (nullable)
     * @return 파싱된 등급, 기본값 BRONZE
     */
    public static UserGrade fromString(String value) {
        if (value == null || value.isBlank()) return BRONZE;
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BRONZE;
        }
    }
}
