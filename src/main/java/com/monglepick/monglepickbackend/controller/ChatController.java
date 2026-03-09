package com.monglepick.monglepickbackend.controller;

import com.monglepick.monglepickbackend.dto.request.ChatRequest;
import com.monglepick.monglepickbackend.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Base64;

/**
 * AI 채팅 컨트롤러
 *
 * <p>사용자의 채팅 요청을 FastAPI AI Agent로 프록시하는 API를 제공합니다.
 * SSE 스트리밍, 동기 응답, 이미지 업로드 3가지 방식을 지원합니다.</p>
 *
 * <p>인증 흐름:</p>
 * <ol>
 *   <li>프론트엔드 → Spring Boot: JWT Bearer 토큰으로 인증</li>
 *   <li>Spring Boot → FastAPI: X-Service-Key 헤더로 서비스 간 인증</li>
 * </ol>
 *
 * <p>SSE 이벤트 타입 (7종):</p>
 * <ul>
 *   <li>session: 세션 ID</li>
 *   <li>status: 처리 상태 (의도 분석 중, 검색 중 등)</li>
 *   <li>movie_card: 추천 영화 카드 JSON</li>
 *   <li>clarification: 추가 질문 (선호도 부족 시)</li>
 *   <li>token: 응답 텍스트 토큰 (스트리밍)</li>
 *   <li>done: 스트리밍 완료</li>
 *   <li>error: 에러 발생</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * SSE 스트리밍 채팅 API
     *
     * <p>AI Agent의 응답을 SSE(Server-Sent Events)로 실시간 스트리밍합니다.
     * 프론트엔드에서 EventSource 또는 fetch API로 SSE를 수신합니다.</p>
     *
     * <p>요청 본문에 base64 인코딩된 이미지를 포함하면
     * VLM(Vision Language Model)이 이미지를 분석하여 영화를 추천합니다.</p>
     *
     * @param request 채팅 요청 (메시지, 이미지, 세션 ID)
     * @param userId JWT에서 추출한 사용자 ID
     * @return SSE 이벤트 스트림을 발행하는 SseEmitter
     */
    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal Long userId) {

        log.info("SSE 채팅 요청 - userId: {}, sessionId: {}, hasImage: {}",
                userId, request.sessionId(), request.image() != null);

        return chatService.streamChat(request, userId);
    }

    /**
     * 동기 채팅 API (디버그/테스트용)
     *
     * <p>SSE 스트리밍 없이 AI Agent의 전체 응답을 JSON으로 즉시 반환합니다.
     * 개발 및 디버깅 시 사용합니다.</p>
     *
     * @param request 채팅 요청 (메시지, 이미지, 세션 ID)
     * @param userId JWT에서 추출한 사용자 ID
     * @return AI Agent의 전체 응답 JSON
     */
    @PostMapping("/sync")
    public ResponseEntity<String> syncChat(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal Long userId) {

        log.info("동기 채팅 요청 - userId: {}, sessionId: {}", userId, request.sessionId());

        String response = chatService.syncChat(request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 이미지 업로드 채팅 API
     *
     * <p>멀티파트 폼 데이터로 이미지 파일을 직접 업로드하여
     * AI Agent에 base64 인코딩된 형태로 전달합니다.</p>
     *
     * <p>지원 이미지 형식: JPEG, PNG, GIF, WebP</p>
     * <p>최대 이미지 크기: Spring Boot 기본 설정 (1MB, 필요 시 조정)</p>
     *
     * @param message 사용자 메시지
     * @param sessionId 세션 ID (선택)
     * @param image 업로드할 이미지 파일
     * @param userId JWT에서 추출한 사용자 ID
     * @return SSE 이벤트 스트림을 발행하는 SseEmitter
     * @throws IOException 이미지 읽기 실패 시
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter uploadChat(
            @RequestPart("message") String message,
            @RequestPart(value = "sessionId", required = false) String sessionId,
            @RequestPart("image") MultipartFile image,
            @AuthenticationPrincipal Long userId) throws IOException {

        log.info("이미지 업로드 채팅 요청 - userId: {}, imageSize: {}KB",
                userId, image.getSize() / 1024);

        // 이미지를 base64로 인코딩하여 ChatRequest 생성
        String base64Image = Base64.getEncoder().encodeToString(image.getBytes());
        ChatRequest request = new ChatRequest(message, base64Image, sessionId);

        return chatService.streamChat(request, userId);
    }
}
