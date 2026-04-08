package com.monglepick.monglepickbackend.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 AI 운영 API DTO 모음.
 *
 * <p>관리자 페이지 "AI 운영" 탭의 엔드포인트(퀴즈 이력/생성 트리거, 챗봇 세션 목록/메시지)를 지원한다.
 * 설계서 {@code docs/관리자페이지_설계서.md} §3.2 AI 운영 참조.</p>
 *
 * <h3>포함 DTO 목록</h3>
 * <ul>
 *   <li>퀴즈: {@link QuizSummary}, {@link GenerateQuizRequest}, {@link GenerateQuizResponse}</li>
 *   <li>챗봇: {@link ChatSessionSummary}, {@link ChatSessionDetail}</li>
 * </ul>
 *
 * <p>2026-04-08: 리뷰 DTO(ReviewSummary/GenerateReviewRequest/GenerateReviewResponse) 제거 — AI 리뷰 생성 기능 삭제.</p>
 */
public final class AdminAiOpsDto {

    /** 인스턴스 생성 방지 (유틸리티 클래스 패턴) */
    private AdminAiOpsDto() {
    }

    // ======================== 퀴즈 ========================

    /**
     * 퀴즈 목록 요약 응답 DTO.
     *
     * <p>관리자 AI 퀴즈 이력 탭에서 한 건의 퀴즈를 표현한다.
     * PENDING/APPROVED/REJECTED/PUBLISHED 전 상태가 노출된다.</p>
     *
     * @param quizId         퀴즈 고유 ID
     * @param movieId        대상 영화 ID (nullable)
     * @param question       퀴즈 문제 텍스트
     * @param correctAnswer  정답 문자열 (관리자 화면에서는 노출 — 검수 목적)
     * @param options        선택지 JSON 문자열 (관리자는 원본 그대로 확인)
     * @param rewardPoint    보상 포인트
     * @param status         퀴즈 상태 (PENDING/APPROVED/REJECTED/PUBLISHED)
     * @param quizDate       출제 예정일 (nullable)
     * @param createdAt      생성 시각
     * @param updatedAt      수정 시각
     */
    public record QuizSummary(
            Long quizId,
            String movieId,
            String question,
            String correctAnswer,
            String options,
            Integer rewardPoint,
            String status,
            String quizDate,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    /**
     * AI 퀴즈 생성 트리거 요청 DTO.
     *
     * <p>관리자가 "AI 퀴즈 생성" 버튼을 클릭하면 호출된다. Agent FastAPI 의 퀴즈 생성 파이프라인을
     * 트리거하여 PENDING 상태의 신규 Quiz 레코드를 생성한다.</p>
     *
     * <h4>현재 구현 상태</h4>
     * <p>Agent 쪽에 전용 admin 엔드포인트가 미구현 상태이므로, Backend 는 요청 파라미터를 기반으로
     * 직접 Quiz 레코드를 PENDING 상태로 INSERT 하는 폴백 경로를 사용한다. 향후 Agent 라우터가 추가되면
     * {@link com.monglepick.monglepickbackend.admin.service.AdminAiOpsService#generateQuiz}에서 HTTP 호출로 전환한다.</p>
     *
     * @param movieId       대상 영화 ID (nullable — 일반 퀴즈는 null)
     * @param question      퀴즈 문제 (필수 — Agent 생성 실패 대비 폴백용으로 관리자가 직접 입력)
     * @param correctAnswer 정답 문자열 (필수)
     * @param options       선택지 JSON 문자열 (필수 — 예: {@code ["A","B","C","D"]})
     * @param explanation   해설 (nullable)
     * @param rewardPoint   보상 포인트 (기본 10P)
     */
    public record GenerateQuizRequest(
            @Size(max = 50, message = "영화 ID는 최대 50자입니다.")
            String movieId,

            @NotBlank(message = "퀴즈 문제는 필수입니다.")
            String question,

            @NotBlank(message = "정답은 필수입니다.")
            @Size(max = 500, message = "정답은 최대 500자입니다.")
            String correctAnswer,

            @NotBlank(message = "선택지 JSON 은 필수입니다.")
            String options,

            String explanation,

            @Min(value = 0, message = "보상 포인트는 0 이상이어야 합니다.")
            Integer rewardPoint
    ) {}

    /**
     * AI 퀴즈 생성 응답 DTO.
     *
     * @param success 생성 성공 여부
     * @param quizId  생성된 퀴즈 ID
     * @param status  초기 상태 (항상 PENDING)
     * @param message 처리 결과 안내 메시지
     */
    public record GenerateQuizResponse(
            boolean success,
            Long quizId,
            String status,
            String message
    ) {}

    // 2026-04-08: 리뷰 섹션(ReviewSummary/GenerateReviewRequest/GenerateReviewResponse) 제거 — AI 리뷰 생성 기능 삭제

    // ======================== 챗봇 세션 ========================

    /**
     * 챗봇 세션 목록 요약 응답 DTO.
     *
     * <p>관리자 AI 운영 탭에서 한 건의 채팅 세션을 표현한다.</p>
     *
     * @param chatSessionArchiveId 아카이브 PK
     * @param sessionId            세션 UUID
     * @param userId               대화 참여 사용자 ID
     * @param title                세션 제목 (nullable)
     * @param turnCount            대화 턴 수
     * @param recommendedMovieCount 추천 영화 수
     * @param startedAt            세션 시작 시각
     * @param lastMessageAt        마지막 메시지 시각
     * @param isActive             활성 여부
     */
    public record ChatSessionSummary(
            Long chatSessionArchiveId,
            String sessionId,
            String userId,
            String title,
            Integer turnCount,
            Integer recommendedMovieCount,
            LocalDateTime startedAt,
            LocalDateTime lastMessageAt,
            Boolean isActive
    ) {}

    /**
     * 챗봇 세션 상세 응답 DTO.
     *
     * <p>세션 한 건의 전체 메시지 내용과 부가 메타데이터(intentSummary 등)를 포함한다.
     * messages/sessionState/intentSummary 는 JSON 문자열 원본 그대로 전달한다 — 프론트엔드에서 파싱.</p>
     *
     * @param sessionId      세션 UUID
     * @param userId         사용자 ID
     * @param title          세션 제목 (nullable)
     * @param messages       전체 메시지 JSON 문자열 (원본)
     * @param sessionState   Agent 세션 상태 JSON (nullable)
     * @param intentSummary  의도 요약 JSON (nullable)
     * @param turnCount      대화 턴 수
     * @param startedAt      시작 시각
     * @param lastMessageAt  마지막 메시지 시각
     * @param isActive       활성 여부
     */
    public record ChatSessionDetail(
            String sessionId,
            String userId,
            String title,
            String messages,
            String sessionState,
            String intentSummary,
            Integer turnCount,
            LocalDateTime startedAt,
            LocalDateTime lastMessageAt,
            Boolean isActive
    ) {}

    /**
     * 챗봇 세션 통계(선택) 응답 DTO — 향후 확장용.
     *
     * @param totalSessions   총 세션 수
     * @param activeSessions  활성 세션 수
     * @param totalTurns      총 대화 턴 수
     * @param topIntents      의도별 빈도 상위 (예: ["recommend", "search", "general"])
     */
    public record ChatStatsResponse(
            long totalSessions,
            long activeSessions,
            long totalTurns,
            List<String> topIntents
    ) {}
}
