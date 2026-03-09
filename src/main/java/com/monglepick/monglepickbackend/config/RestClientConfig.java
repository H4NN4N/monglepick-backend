package com.monglepick.monglepickbackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * RestClient 설정 클래스
 *
 * <p>FastAPI AI Agent 서버와 통신하기 위한 RestClient 빈을 구성합니다.
 * Spring Boot 4.x에서 권장하는 RestClient를 사용하며,
 * 기본 URL과 타임아웃을 application.yml에서 주입받습니다.</p>
 *
 * <p>AI Agent 호출 시 X-Service-Key 헤더를 자동으로 추가합니다.</p>
 */
@Configuration
public class RestClientConfig {

    /** FastAPI AI Agent 서버의 기본 URL (기본값: http://localhost:8000) */
    @Value("${ai.agent.base-url}")
    private String aiAgentBaseUrl;

    /** 서비스 간 인증 키 (X-Service-Key 헤더로 전송) */
    @Value("${ai.agent.service-key}")
    private String aiAgentServiceKey;

    /** AI Agent 호출 타임아웃 (초 단위) */
    @Value("${ai.agent.timeout}")
    private int aiAgentTimeout;

    /**
     * AI Agent 전용 RestClient 빈 생성
     *
     * <p>기본 URL, 서비스 인증 헤더, 타임아웃을 사전 설정하여
     * 매 호출마다 반복 설정할 필요 없이 바로 사용할 수 있습니다.</p>
     *
     * <p>타임아웃 설정:</p>
     * <ul>
     *   <li>연결 타임아웃: 5초 (고정)</li>
     *   <li>읽기 타임아웃: application.yml의 ai.agent.timeout 값 (기본 60초)</li>
     * </ul>
     *
     * @return AI Agent 통신용 RestClient 인스턴스
     */
    @Bean(name = "aiAgentRestClient")
    public RestClient aiAgentRestClient() {
        // HTTP 요청 팩토리 설정 (타임아웃)
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        // 서버 연결 타임아웃: 5초
        requestFactory.setConnectTimeout(5000);
        // 응답 읽기 타임아웃: AI Agent는 LLM 처리로 인해 오래 걸릴 수 있음
        requestFactory.setReadTimeout(aiAgentTimeout * 1000);

        return RestClient.builder()
                // 기본 URL 설정 (예: http://localhost:8000)
                .baseUrl(aiAgentBaseUrl)
                // 모든 요청에 X-Service-Key 헤더 자동 추가
                .defaultHeader("X-Service-Key", aiAgentServiceKey)
                // Content-Type 기본값: JSON
                .defaultHeader("Content-Type", "application/json")
                // 타임아웃이 적용된 요청 팩토리 설정
                .requestFactory(requestFactory)
                .build();
    }
}
