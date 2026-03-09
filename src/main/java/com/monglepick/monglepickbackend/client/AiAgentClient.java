package com.monglepick.monglepickbackend.client;

import com.monglepick.monglepickbackend.exception.BusinessException;
import com.monglepick.monglepickbackend.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * FastAPI AI Agent 통신 클라이언트
 *
 * <p>Spring Boot 백엔드와 FastAPI AI Agent 서버 간의 통신을 담당합니다.
 * RestClient를 사용하여 동기/SSE 요청을 수행하며,
 * 모든 요청에 X-Service-Key 헤더가 자동으로 추가됩니다.</p>
 *
 * <p>지원하는 AI Agent API:</p>
 * <ul>
 *   <li>POST /api/v1/chat - SSE 스트리밍 채팅</li>
 *   <li>POST /api/v1/chat/sync - 동기 채팅 (디버그용)</li>
 *   <li>POST /api/v1/chat/upload - 이미지 업로드 채팅</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiAgentClient {

    /** AI Agent 전용 RestClient (기본 URL, 서비스 키 사전 설정됨) */
    @Qualifier("aiAgentRestClient")
    private final RestClient aiAgentRestClient;

    /**
     * AI Agent에 동기 채팅 요청을 전송합니다.
     *
     * <p>POST /api/v1/chat/sync 엔드포인트를 호출하여
     * AI의 응답을 JSON 형태로 즉시 반환받습니다.</p>
     *
     * @param requestBody 채팅 요청 본문 (message, image, session_id 포함)
     * @return AI Agent의 JSON 응답 문자열
     * @throws BusinessException AI Agent 통신 실패 시
     */
    public String sendSyncChat(Map<String, Object> requestBody) {
        try {
            log.debug("AI Agent 동기 채팅 요청 전송 - sessionId: {}",
                    requestBody.get("session_id"));

            String response = aiAgentRestClient
                    .post()
                    .uri("/api/v1/chat/sync")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            log.debug("AI Agent 동기 채팅 응답 수신 완료");
            return response;

        } catch (RestClientException e) {
            log.error("AI Agent 동기 채팅 통신 실패: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.AI_AGENT_CONNECTION_FAILED, e);
        }
    }

    /**
     * AI Agent에 SSE 스트리밍 채팅 요청을 전송하고 SseEmitter로 프록시합니다.
     *
     * <p>POST /api/v1/chat 엔드포인트를 호출하여 SSE 스트림을 받고,
     * 이를 클라이언트의 SseEmitter로 그대로 전달합니다.</p>
     *
     * <p>SSE 이벤트 타입 (7종):</p>
     * <ul>
     *   <li>session: 세션 ID 전달</li>
     *   <li>status: 처리 상태 업데이트</li>
     *   <li>movie_card: 추천 영화 카드 데이터</li>
     *   <li>clarification: 추가 질문</li>
     *   <li>token: 응답 텍스트 토큰</li>
     *   <li>done: 스트리밍 완료</li>
     *   <li>error: 에러 발생</li>
     * </ul>
     *
     * @param requestBody 채팅 요청 본문
     * @param emitter SSE 이벤트를 전달할 SseEmitter
     */
    public void sendStreamingChat(Map<String, Object> requestBody, SseEmitter emitter) {
        try {
            log.debug("AI Agent SSE 스트리밍 채팅 요청 전송 - sessionId: {}",
                    requestBody.get("session_id"));

            // AI Agent로부터 SSE 스트림을 InputStream으로 수신
            InputStream inputStream = aiAgentRestClient
                    .post()
                    .uri("/api/v1/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(InputStream.class);

            if (inputStream == null) {
                log.error("AI Agent SSE 스트림이 null입니다.");
                emitter.completeWithError(
                        new BusinessException(ErrorCode.AI_AGENT_RESPONSE_ERROR));
                return;
            }

            // SSE 스트림을 줄 단위로 읽어 클라이언트에게 전달
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

                String line;
                StringBuilder eventData = new StringBuilder();
                String eventType = null;

                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("event:")) {
                        // SSE 이벤트 타입 추출 (예: "event: token")
                        eventType = line.substring(6).trim();
                    } else if (line.startsWith("data:")) {
                        // SSE 데이터 추출
                        eventData.append(line.substring(5).trim());
                    } else if (line.isEmpty() && eventData.length() > 0) {
                        // 빈 줄 = 하나의 SSE 이벤트 완성, 클라이언트에게 전달
                        if (eventType != null) {
                            emitter.send(SseEmitter.event()
                                    .name(eventType)
                                    .data(eventData.toString()));
                        } else {
                            emitter.send(SseEmitter.event()
                                    .data(eventData.toString()));
                        }
                        // 다음 이벤트를 위해 초기화
                        eventData.setLength(0);
                        eventType = null;
                    }
                }
            }

            // 스트리밍 완료
            emitter.complete();
            log.debug("AI Agent SSE 스트리밍 완료");

        } catch (Exception e) {
            log.error("AI Agent SSE 스트리밍 중 오류 발생: {}", e.getMessage(), e);
            emitter.completeWithError(e);
        }
    }

    /**
     * AI Agent 서버의 헬스 상태를 확인합니다.
     *
     * <p>GET /health 엔드포인트를 호출하여 AI Agent가 정상 동작 중인지 확인합니다.</p>
     *
     * @return AI Agent가 정상이면 true
     */
    public boolean checkHealth() {
        try {
            String response = aiAgentRestClient
                    .get()
                    .uri("/health")
                    .retrieve()
                    .body(String.class);

            log.debug("AI Agent 헬스체크 응답: {}", response);
            return response != null;

        } catch (Exception e) {
            log.warn("AI Agent 헬스체크 실패: {}", e.getMessage());
            return false;
        }
    }
}
