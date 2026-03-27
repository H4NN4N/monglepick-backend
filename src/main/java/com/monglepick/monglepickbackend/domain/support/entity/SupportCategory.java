package com.monglepick.monglepickbackend.domain.support.entity;

/**
 * 고객센터 카테고리 열거형.
 *
 * <p>FAQ, 도움말 문서, 상담 티켓에서 공통으로 사용한다.
 * JPA EnumType.STRING으로 DB에 문자열 그대로 저장된다.</p>
 *
 * <ul>
 *   <li>{@link #GENERAL}        — 일반 문의 (서비스 전반)</li>
 *   <li>{@link #ACCOUNT}        — 계정/회원 관련 (로그인, 비밀번호, 탈퇴)</li>
 *   <li>{@link #CHAT}           — AI 채팅 사용 방법 및 오류</li>
 *   <li>{@link #RECOMMENDATION} — 영화 추천 기능 관련</li>
 *   <li>{@link #COMMUNITY}      — 커뮤니티 게시판 관련</li>
 *   <li>{@link #PAYMENT}        — 결제/구독/포인트 관련</li>
 * </ul>
 */
public enum SupportCategory {

    /** 일반 문의 — 서비스 전반, 기타 문의 */
    GENERAL,

    /** 계정 관련 — 로그인, 비밀번호 재설정, 회원 탈퇴 */
    ACCOUNT,

    /** AI 채팅 관련 — 채팅 사용법, 쿼터, 오류 */
    CHAT,

    /** 영화 추천 관련 — 추천 알고리즘, 개인화 설정 */
    RECOMMENDATION,

    /** 커뮤니티 관련 — 게시글/댓글 작성, 신고 */
    COMMUNITY,

    /** 결제/구독/포인트 관련 — 결제 오류, 환불, 포인트 적립/사용 */
    PAYMENT
}
