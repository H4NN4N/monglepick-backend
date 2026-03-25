package com.monglepick.monglepickbackend.global.constants;

/**
 * 포인트 변동 유형 열거형.
 *
 * <p>포인트 이력(points_history) 테이블의 point_type 컬럼에 저장되며,
 * 변동의 원인을 분류한다.</p>
 *
 * @see com.monglepick.monglepickbackend.domain.reward.entity.PointsHistory
 */
public enum PointType {

    /** 포인트 획득 (출석 체크, 퀴즈, 이벤트 등) */
    EARN("earn"),

    /** 포인트 사용 (AI 추천, 아이템 교환 등) */
    SPEND("spend"),

    /** 포인트 만료 (유효기간 도래) */
    EXPIRE("expire"),

    /** 보너스 포인트 (회원가입, 프로모션 등) */
    BONUS("bonus");

    /** DB에 저장되는 문자열 값 */
    private final String value;

    PointType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
