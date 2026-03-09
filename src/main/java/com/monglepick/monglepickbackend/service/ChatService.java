package com.monglepick.monglepickbackend.service;

import com.monglepick.monglepickbackend.client.AiAgentClient;
import com.monglepick.monglepickbackend.dto.request.ChatRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 채팅 서비스
 *
 * <p>사용자의 채팅 요청을 FastAPI AI Agent로 프록시합니다.
 * SSE 스트리밍과 동기 요청을 모두 지원합니다.</p>
 *
 * <p>프록시 과정:</p>
 * <ol>
 *   <li>프론트엔드 → Spring Boot (JWT 인증)</li>
 *   <li>Spring Boot → FastAPI AI Agent (X-Service-Key 인증)</li>
 *   <li>FastAPI AI Agent → Spring Boot (SSE 스트림)</li>
 *   <li>Spring Boot → 프론트엔드 (SSE 스트림 전달)</li>
 * </ol>
 *
 * <p>SSE 이벤트 타입 (7종): session, status, movie_card, clarification, token, done, error</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final AiAgentClient aiAgentClient;

    /** SSE 스트리밍을 비동기로 처리하기 위한 스레드 풀 */
    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();

    /** SSE 타임아웃: 5분 (AI Agent의 LLM 처리 시간 고려) */
    private static final long SSE_TIMEOUT = 300_000L;

    /**
     * SSE 스트리밍 채팅을 수행합니다.
     *
     * <p>SseEmitter를 생성하여 클라이언트에게 반환하고,
     * 별도 스레드에서 AI Agent와 SSE 스트리밍 통신을 수행합니다.
     * AI Agent의 SSE 이벤트를 그대로 클라이언트에게 전달합니다.</p>
     *
     * @param request 채팅 요청 (메시지, 이미지, 세션 ID)
     * @param userId 인증된 사용자 ID (JWT에서 추출)
     * @return SSE 이벤트를 발행하는 SseEmitter
     */
    public SseEmitter streamChat(ChatRequest request, Long userId) {
        // 1. SseEmitter 생성 (5분 타임아웃)
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // 2. AI Agent 요청 본문 구성
        Map<String, Object> requestBody = buildRequestBody(request, userId);

        // 3. 비동기로 AI Agent SSE 스트리밍 실행
        sseExecutor.execute(() -> {
            try {
                log.info("SSE 스트리밍 시작 - userId: {}, sessionId: {}",
                        userId, request.sessionId());
                aiAgentClient.sendStreamingChat(requestBody, emitter);
            } catch (Exception e) {
                log.error("SSE 스트리밍 중 예외 발생 - userId: {}: {}",
                        userId, e.getMessage(), e);
                emitter.completeWithError(e);
            }
        });

        // 4. 타임아웃/에러/완료 콜백 등록
        emitter.onTimeout(() -> {
            log.warn("SSE 스트리밍 타임아웃 - userId: {}", userId);
            emitter.complete();
        });

        emitter.onError(throwable -> {
            log.error("SSE 스트리밍 에러 - userId: {}: {}",
                    userId, throwable.getMessage());
        });

        return emitter;
    }

    /**
     * 동기 채팅을 수행합니다.
     *
     * <p>디버그 및 테스트용으로, AI Agent의 응답을 JSON으로 즉시 반환합니다.
     * SSE 스트리밍 없이 전체 응답을 한 번에 받습니다.</p>
     *
     * @param request 채팅 요청 (메시지, 이미지, 세션 ID)
     * @param userId 인증된 사용자 ID
     * @return AI Agent의 JSON 응답 문자열
     */
    public String syncChat(ChatRequest request, Long userId) {
        Map<String, Object> requestBody = buildRequestBody(request, userId);

        log.info("동기 채팅 요청 - userId: {}, sessionId: {}", userId, request.sessionId());
        String response = aiAgentClient.sendSyncChat(requestBody);
        log.info("동기 채팅 응답 수신 - userId: {}", userId);

        return response;
    }

    /**
     * AI Agent 요청 본문을 구성합니다.
     *
     * <p>프론트엔드의 ChatRequest를 AI Agent가 기대하는 형식으로 변환합니다.
     * user_id를 추가하여 AI Agent가 사용자별 세션을 관리할 수 있도록 합니다.</p>
     *
     * @param request 채팅 요청
     * @param userId 사용자 ID
     * @return AI Agent 요청 본문 Map
     */
    private Map<String, Object> buildRequestBody(ChatRequest request, Long userId) {
        Map<String, Object> body = new HashMap<>();

        // 필수: 사용자 메시지
        body.put("message", request.message());

        // 필수: 사용자 ID (AI Agent가 사용자별 추천을 위해 사용)
        body.put("user_id", userId.toString());

        // 선택: 세션 ID (멀티턴 대화 유지용, null이면 AI Agent가 새로 생성)
        if (request.sessionId() != null && !request.sessionId().isBlank()) {
            body.put("session_id", request.sessionId());
        }

        // 선택: base64 인코딩된 이미지 (VLM 이미지 분석용)
        if (request.image() != null && !request.image().isBlank()) {
            body.put("image", request.image());
        }

        return body;
    }
}
