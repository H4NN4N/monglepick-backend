package com.monglepick.monglepickbackend.admin.service;

import com.monglepick.monglepickbackend.admin.dto.AdminAiOpsDto.ChatSessionDetail;
import com.monglepick.monglepickbackend.admin.dto.AdminAiOpsDto.ChatSessionSummary;
import com.monglepick.monglepickbackend.admin.dto.AdminAiOpsDto.GenerateQuizRequest;
import com.monglepick.monglepickbackend.admin.dto.AdminAiOpsDto.GenerateQuizResponse;
import com.monglepick.monglepickbackend.admin.dto.AdminAiOpsDto.QuizSummary;
import com.monglepick.monglepickbackend.admin.repository.AdminChatSessionRepository;
import com.monglepick.monglepickbackend.admin.repository.AdminQuizRepository;
import com.monglepick.monglepickbackend.domain.chat.entity.ChatSessionArchive;
import com.monglepick.monglepickbackend.domain.roadmap.entity.Quiz;
import com.monglepick.monglepickbackend.global.exception.BusinessException;
import com.monglepick.monglepickbackend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 AI 운영 서비스.
 *
 * <p>관리자 페이지 "AI 운영" 탭의 비즈니스 로직을 담당한다.
 * 설계서 {@code docs/관리자페이지_설계서.md} §3.2 AI 운영 범위.</p>
 *
 * <h3>담당 기능 (4개)</h3>
 * <ul>
 *   <li>퀴즈: 이력 조회 / 생성 트리거 (2)</li>
 *   <li>챗봇: 세션 목록 / 세션 메시지 (2)</li>
 * </ul>
 *
 * <p>2026-04-08: AI 리뷰 생성/이력 기능 제거 (ai_generated 플래그 부재로 의미 없음).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAiOpsService {

    /** 관리자 전용 퀴즈 리포지토리 — 페이징 + 상태 필터 */
    private final AdminQuizRepository adminQuizRepository;

    /** 관리자 전용 채팅 세션 리포지토리 */
    private final AdminChatSessionRepository adminChatSessionRepository;
    // 2026-04-08: reviewMapper / agentRestClient / agentUrl 제거 — AI 리뷰 생성 기능 삭제

    // ======================== 퀴즈 ========================

    /**
     * 퀴즈 이력을 최신순으로 페이징 조회한다.
     *
     * <p>status 파라미터가 null/공백이면 전체, 그 외에는 해당 상태만 필터링한다.</p>
     *
     * @param status   퀴즈 상태 문자열 (PENDING/APPROVED/REJECTED/PUBLISHED)
     * @param pageable 페이지 정보
     * @return 퀴즈 요약 페이지
     */
    public Page<QuizSummary> getQuizHistory(String status, Pageable pageable) {
        log.debug("[AdminAiOps] 퀴즈 이력 조회 — status={}, page={}", status, pageable.getPageNumber());

        if (status != null && !status.isBlank()) {
            Quiz.QuizStatus statusEnum;
            try {
                statusEnum = Quiz.QuizStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("[AdminAiOps] 잘못된 퀴즈 상태 필터: {}", status);
                throw new BusinessException(ErrorCode.INVALID_INPUT,
                        "허용되지 않은 퀴즈 상태: " + status);
            }
            return adminQuizRepository.findByStatusOrderByCreatedAtDesc(statusEnum, pageable)
                    .map(this::toQuizSummary);
        }

        return adminQuizRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toQuizSummary);
    }

    /**
     * AI 퀴즈 생성을 트리거한다.
     *
     * <p>현재 구현에서는 Agent 라우터가 미구현이므로, 요청 파라미터를 그대로 받아
     * PENDING 상태의 Quiz 레코드를 직접 INSERT 한다. 관리자는 검수 후 approve() 로 전환한다.</p>
     *
     * <p>TODO: Agent 쪽 {@code POST /admin/ai/quiz/generate} 엔드포인트가 추가되면
     * 이 메서드를 HTTP 호출로 전환한다.</p>
     *
     * @param request 생성 요청 DTO (문제/정답/선택지 필수)
     * @return 생성 결과 응답 DTO
     */
    @Transactional
    public GenerateQuizResponse generateQuiz(GenerateQuizRequest request) {
        log.info("[AdminAiOps] 퀴즈 생성 요청 — movieId={}, question preview={}",
                request.movieId(),
                request.question().length() > 30 ? request.question().substring(0, 30) + "…" : request.question());

        Quiz quiz = Quiz.builder()
                .movieId(request.movieId())
                .question(request.question())
                .correctAnswer(request.correctAnswer())
                .options(request.options())
                .explanation(request.explanation())
                .rewardPoint(request.rewardPoint() != null ? request.rewardPoint() : 10)
                .status(Quiz.QuizStatus.PENDING)
                .build();

        Quiz saved = adminQuizRepository.save(quiz);
        log.info("[AdminAiOps] 퀴즈 생성 완료 — quizId={}", saved.getQuizId());

        return new GenerateQuizResponse(
                true,
                saved.getQuizId(),
                saved.getStatus().name(),
                "퀴즈가 PENDING 상태로 등록되었습니다. 검수 후 APPROVED 로 전환하세요."
        );
    }

    // ======================== 챗봇 세션 ========================
    // 2026-04-08: AI 리뷰 이력/생성 기능 제거 — ai_generated 플래그 부재로 의미 없음

    /**
     * 전체 채팅 세션 목록을 최신순으로 페이징 조회한다.
     *
     * @param pageable 페이지 정보
     * @return 세션 요약 페이지 (소프트 삭제 제외)
     */
    public Page<ChatSessionSummary> getChatSessions(Pageable pageable) {
        log.debug("[AdminAiOps] 챗봇 세션 목록 조회 — page={}", pageable.getPageNumber());

        return adminChatSessionRepository
                .findByIsDeletedFalseOrderByLastMessageAtDesc(pageable)
                .map(this::toChatSessionSummary);
    }

    /**
     * 단일 채팅 세션의 메시지 상세를 조회한다.
     *
     * @param sessionId 세션 UUID
     * @return 세션 상세 응답 DTO
     * @throws BusinessException 세션 미발견 시
     */
    public ChatSessionDetail getChatSessionDetail(String sessionId) {
        log.debug("[AdminAiOps] 챗봇 세션 상세 조회 — sessionId={}", sessionId);

        ChatSessionArchive archive = adminChatSessionRepository
                .findBySessionIdAndIsDeletedFalse(sessionId)
                .orElseThrow(() -> {
                    log.warn("[AdminAiOps] 챗봇 세션 상세 실패 — 미발견: sessionId={}", sessionId);
                    return new BusinessException(ErrorCode.INVALID_INPUT,
                            "채팅 세션을 찾을 수 없습니다: " + sessionId);
                });

        // ChatSessionArchive는 String FK 직접 보관 (JPA/MyBatis 하이브리드 §15.4)
        String userId = archive.getUserId();

        return new ChatSessionDetail(
                archive.getSessionId(),
                userId,
                archive.getTitle(),
                archive.getMessages(),
                archive.getSessionState(),
                archive.getIntentSummary(),
                archive.getTurnCount(),
                archive.getStartedAt(),
                archive.getLastMessageAt(),
                archive.getIsActive()
        );
    }

    // ======================== DTO 변환 ========================

    /**
     * {@link Quiz} → {@link QuizSummary} 응답 DTO.
     */
    private QuizSummary toQuizSummary(Quiz quiz) {
        return new QuizSummary(
                quiz.getQuizId(),
                quiz.getMovieId(),
                quiz.getQuestion(),
                quiz.getCorrectAnswer(),
                quiz.getOptions(),
                quiz.getRewardPoint(),
                quiz.getStatus().name(),
                quiz.getQuizDate() != null ? quiz.getQuizDate().toString() : null,
                quiz.getCreatedAt(),
                quiz.getUpdatedAt()
        );
    }

    /**
     * {@link ChatSessionArchive} → {@link ChatSessionSummary} 응답 DTO.
     *
     * <p>ChatSessionArchive 는 String FK 직접 보관 방식이므로 LAZY 프록시 없이
     * 곧바로 userId 를 읽는다 (JPA/MyBatis 하이브리드 §15.4).</p>
     */
    private ChatSessionSummary toChatSessionSummary(ChatSessionArchive archive) {
        String userId = archive.getUserId();
        return new ChatSessionSummary(
                archive.getChatSessionArchiveId(),
                archive.getSessionId(),
                userId,
                archive.getTitle(),
                archive.getTurnCount(),
                archive.getRecommendedMovieCount(),
                archive.getStartedAt(),
                archive.getLastMessageAt(),
                archive.getIsActive()
        );
    }
}
