package com.monglepick.monglepickbackend.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 웹 MVC 설정 클래스
 *
 * <p>CORS(Cross-Origin Resource Sharing) 정책을 설정하여
 * 프론트엔드(monglepick-client)에서 백엔드 API에 접근할 수 있도록 허용합니다.</p>
 *
 * <p>application.yml의 cors 섹션에서 설정값을 주입받으며,
 * 설정이 없는 경우 기본값을 사용합니다.</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** 허용할 프론트엔드 오리진 목록 (쉼표 구분, 환경변수로 오버라이드 가능) */
    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String[] allowedOrigins;

    /** 허용할 HTTP 메서드 목록 */
    @Value("${cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}")
    private String[] allowedMethods;

    /** 허용할 요청 헤더 */
    @Value("${cors.allowed-headers:*}")
    private String allowedHeaders;

    /** 자격증명(쿠키, Authorization) 허용 여부 */
    @Value("${cors.allow-credentials:true}")
    private boolean allowCredentials;

    /** preflight 요청 캐시 시간 (초 단위) */
    @Value("${cors.max-age:3600}")
    private long maxAge;

    /**
     * CORS 매핑 설정
     *
     * <p>모든 API 경로(/api/**)에 대해 CORS 정책을 적용합니다.
     * 프론트엔드에서 JWT 토큰을 Authorization 헤더로 전송하므로
     * credentials를 허용하고, 모든 헤더를 수락합니다.</p>
     *
     * @param registry CORS 레지스트리
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods(allowedMethods)
                .allowedHeaders(allowedHeaders)
                .allowCredentials(allowCredentials)
                .maxAge(maxAge);
    }
}
