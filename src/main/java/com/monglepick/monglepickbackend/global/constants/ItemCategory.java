package com.monglepick.monglepickbackend.global.constants;

/**
 * 포인트 아이템 카테고리 열거형.
 *
 * <p>포인트 상점에서 교환 가능한 아이템의 분류를 정의한다.</p>
 *
 * @see com.monglepick.monglepickbackend.domain.reward.entity.PointItem
 */
public enum ItemCategory {

    /** 일반 아이템 (기본 카테고리) */
    GENERAL("general"),

    /** 쿠폰형 아이템 */
    COUPON("coupon"),

    /** 아바타/프로필 꾸미기 아이템 */
    AVATAR("avatar"),

    /** AI 추천 이용권 관련 아이템 */
    AI("ai");

    /** DB에 저장되는 소문자 문자열 값 */
    private final String value;

    ItemCategory(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * 문자열로부터 카테고리를 안전하게 파싱한다.
     * null이거나 알 수 없는 값이면 GENERAL을 반환한다.
     *
     * @param value 카테고리 문자열 (nullable)
     * @return 파싱된 카테고리, 기본값 GENERAL
     */
    public static ItemCategory fromString(String value) {
        if (value == null || value.isBlank()) return GENERAL;
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return GENERAL;
        }
    }
}
