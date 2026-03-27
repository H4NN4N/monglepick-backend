package com.monglepick.monglepickbackend.domain.support.dto;

import com.monglepick.monglepickbackend.domain.support.entity.SupportFaq;
import com.monglepick.monglepickbackend.domain.support.entity.SupportHelpArticle;
import com.monglepick.monglepickbackend.domain.support.entity.SupportTicket;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 고객센터(Support) 도메인 DTO 모음.
 *
 * <p>모든 DTO는 불변 Java record 타입으로 선언된다.
 * 각 record는 해당 엔티티로부터 정적 팩토리 메서드 {@code from(Entity)}를 통해 생성된다.</p>
 *
 * <h3>포함된 DTO</h3>
 * <ul>
 *   <li>{@link FaqResponse}           — FAQ 단건/목록 응답</li>
 *   <li>{@link HelpArticleResponse}   — 도움말 문서 단건/목록 응답</li>
 *   <li>{@link TicketCreateRequest}   — 상담 티켓 생성 요청</li>
 *   <li>{@link TicketResponse}        — 상담 티켓 응답 (목록/단건)</li>
 *   <li>{@link FaqFeedbackRequest}    — FAQ 피드백 요청</li>
 * </ul>
 */
public class SupportDto {

    /** 외부에서 SupportDto 인스턴스 생성을 막는 private 생성자. */
    private SupportDto() {}

    // ─────────────────────────────────────────────
    // FAQ 응답 DTO
    // ─────────────────────────────────────────────

    /**
     * FAQ 단건 응답 DTO.
     *
     * <p>목록 조회({@code GET /api/v1/support/faqs})와
     * 단건 조회({@code GET /api/v1/support/faqs/{id}}) 모두 이 record를 사용한다.</p>
     *
     * @param id             FAQ ID
     * @param category       카테고리 문자열 (예: "PAYMENT", "ACCOUNT")
     * @param question       질문 내용
     * @param answer         답변 내용
     * @param helpfulCount   "도움됨" 피드백 수
     * @param notHelpfulCount "도움 안됨" 피드백 수
     */
    public record FaqResponse(
            Long id,
            String category,
            String question,
            String answer,
            int helpfulCount,
            int notHelpfulCount
    ) {
        /**
         * {@link SupportFaq} 엔티티로부터 FaqResponse를 생성하는 정적 팩토리 메서드.
         *
         * @param faq FAQ 엔티티
         * @return FaqResponse 인스턴스
         */
        public static FaqResponse from(SupportFaq faq) {
            return new FaqResponse(
                    faq.getFaqId(),
                    faq.getCategory().name(),      // enum → 문자열 (예: "PAYMENT")
                    faq.getQuestion(),
                    faq.getAnswer(),
                    faq.getHelpfulCount(),
                    faq.getNotHelpfulCount()
            );
        }
    }

    // ─────────────────────────────────────────────
    // 도움말 문서 응답 DTO
    // ─────────────────────────────────────────────

    /**
     * 도움말 문서 응답 DTO.
     *
     * <p>목록 조회({@code GET /api/v1/support/help})와
     * 단건 조회({@code GET /api/v1/support/help/{id}}) 모두 이 record를 사용한다.
     * 단건 조회 시에는 조회수 증가가 함께 처리된다.</p>
     *
     * @param id        도움말 문서 ID
     * @param category  카테고리 문자열
     * @param title     문서 제목
     * @param content   문서 본문
     * @param viewCount 조회수
     */
    public record HelpArticleResponse(
            Long id,
            String category,
            String title,
            String content,
            int viewCount
    ) {
        /**
         * {@link SupportHelpArticle} 엔티티로부터 HelpArticleResponse를 생성하는 정적 팩토리 메서드.
         *
         * @param article 도움말 문서 엔티티
         * @return HelpArticleResponse 인스턴스
         */
        public static HelpArticleResponse from(SupportHelpArticle article) {
            return new HelpArticleResponse(
                    article.getArticleId(),
                    article.getCategory().name(),  // enum → 문자열
                    article.getTitle(),
                    article.getContent(),
                    article.getViewCount()
            );
        }
    }

    // ─────────────────────────────────────────────
    // 티켓 생성 요청 DTO
    // ─────────────────────────────────────────────

    /**
     * 상담 티켓 생성 요청 DTO.
     *
     * <p>{@code POST /api/v1/support/tickets} 요청 바디.</p>
     *
     * @param category 문의 카테고리 문자열 — SupportCategory enum 이름과 일치해야 함
     *                 (GENERAL / ACCOUNT / CHAT / RECOMMENDATION / COMMUNITY / PAYMENT)
     * @param title    문의 제목 — 공백 불가, 2자 이상 100자 이하
     * @param content  문의 내용 — 공백 불가, 10자 이상 2000자 이하
     */
    public record TicketCreateRequest(
            @NotBlank(message = "카테고리를 선택해주세요")
            String category,

            @NotBlank(message = "제목을 입력해주세요")
            @Size(min = 2, max = 100, message = "제목은 2자 이상 100자 이하로 입력해주세요")
            String title,

            @NotBlank(message = "문의 내용을 입력해주세요")
            @Size(min = 10, max = 2000, message = "문의 내용은 10자 이상 2000자 이하로 입력해주세요")
            String content
    ) {}

    // ─────────────────────────────────────────────
    // 티켓 응답 DTO
    // ─────────────────────────────────────────────

    /**
     * 상담 티켓 응답 DTO.
     *
     * <p>티켓 목록({@code GET /api/v1/support/tickets})과
     * 티켓 생성({@code POST /api/v1/support/tickets}) 응답 모두 이 record를 사용한다.</p>
     *
     * @param ticketId  티켓 ID
     * @param title     문의 제목
     * @param category  카테고리 문자열
     * @param status    처리 상태 문자열 (OPEN / IN_PROGRESS / RESOLVED / CLOSED)
     * @param createdAt 티켓 생성 시각
     */
    public record TicketResponse(
            Long ticketId,
            String title,
            String category,
            String status,
            LocalDateTime createdAt
    ) {
        /**
         * {@link SupportTicket} 엔티티로부터 TicketResponse를 생성하는 정적 팩토리 메서드.
         *
         * @param ticket 상담 티켓 엔티티
         * @return TicketResponse 인스턴스
         */
        public static TicketResponse from(SupportTicket ticket) {
            return new TicketResponse(
                    ticket.getTicketId(),
                    ticket.getTitle(),
                    ticket.getCategory().name(),   // enum → 문자열
                    ticket.getStatus().name(),      // enum → 문자열
                    ticket.getCreatedAt()
            );
        }
    }

    // ─────────────────────────────────────────────
    // FAQ 피드백 요청 DTO
    // ─────────────────────────────────────────────

    /**
     * FAQ 피드백 요청 DTO.
     *
     * <p>{@code POST /api/v1/support/faqs/{id}/feedback} 요청 바디.</p>
     *
     * @param helpful true: "도움됨", false: "도움 안됨"
     */
    public record FaqFeedbackRequest(
            boolean helpful
    ) {}
}
