package com.monglepick.monglepickbackend.domain.support.entity;

/**
 * 상담 티켓 처리 상태 열거형.
 *
 * <p>SupportTicket 엔티티의 status 필드에서 사용한다.
 * JPA EnumType.STRING으로 DB에 문자열 그대로 저장된다.</p>
 *
 * <ul>
 *   <li>{@link #OPEN}        — 접수 완료, 처리 대기 중 (기본값)</li>
 *   <li>{@link #IN_PROGRESS} — 담당자가 처리 중</li>
 *   <li>{@link #RESOLVED}    — 처리 완료, 사용자 확인 대기</li>
 *   <li>{@link #CLOSED}      — 종결 (사용자 확인 완료 또는 자동 종결)</li>
 * </ul>
 */
public enum TicketStatus {

    /** 접수 완료 — 처리 대기 중 (티켓 생성 시 기본값) */
    OPEN,

    /** 처리 중 — 담당자가 응답을 작성하거나 조사 중 */
    IN_PROGRESS,

    /** 처리 완료 — 답변 완료, 사용자 확인 대기 */
    RESOLVED,

    /** 종결 — 사용자가 확인 완료하거나 일정 기간 후 자동 종결 */
    CLOSED
}
