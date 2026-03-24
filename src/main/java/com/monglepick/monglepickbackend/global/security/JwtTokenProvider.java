package com.monglepick.monglepickbackend.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 생성 및 검증을 담당하는 프로바이더.
 *
 * <p>JJWT 0.12.6 라이브러리를 사용하여 Access Token과 Refresh Token을
 * 생성하고, 토큰의 유효성 검증 및 클레임 추출 기능을 제공한다.</p>
 *
 * <h3>토큰 구조</h3>
 * <ul>
 *   <li><b>Access Token</b>: subject=userId, role 클레임, 만료시간 1시간(기본)</li>
 *   <li><b>Refresh Token</b>: subject=userId, type="refresh" 클레임, 만료시간 7일(기본)</li>
 * </ul>
 *
 * <h3>서명 알고리즘</h3>
 * <p>HMAC-SHA256 (HS256)을 사용하며, 최소 256비트(32바이트) 이상의 시크릿 키가 필요하다.</p>
 *
 * @see JwtAuthenticationFilter
 * @see SecurityConfig
 */
@Slf4j
@Component
public class JwtTokenProvider {

    /** application.yml의 app.jwt.secret에서 주입받는 JWT 서명용 시크릿 문자열 */
    @Value("${app.jwt.secret}")
    private String secret;

    /** Access Token 만료 시간 (밀리초, 기본 1시간 = 3,600,000ms) */
    @Value("${app.jwt.access-token-expiry}")
    private long accessTokenExpiry;

    /** Refresh Token 만료 시간 (밀리초, 기본 7일 = 604,800,000ms) */
    @Value("${app.jwt.refresh-token-expiry}")
    private long refreshTokenExpiry;

    /** HMAC-SHA256 서명에 사용할 SecretKey (PostConstruct에서 초기화) */
    private SecretKey key;

    /**
     * 빈 초기화 시 시크릿 문자열로부터 HMAC-SHA256 키를 생성한다.
     *
     * <p>시크릿 문자열을 UTF-8 바이트로 변환한 후
     * {@code Keys.hmacShaKeyFor()}로 SecretKey 인스턴스를 생성한다.</p>
     */
    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        log.info("JWT 토큰 프로바이더 초기화 완료 (AccessToken 만료: {}ms, RefreshToken 만료: {}ms)",
                accessTokenExpiry, refreshTokenExpiry);
    }

    /**
     * Access Token을 생성한다.
     *
     * <p>JWT payload에 subject(userId)와 role 클레임을 포함하며,
     * 발급 시각(iat)과 만료 시각(exp)이 자동으로 설정된다.</p>
     *
     * @param userId 사용자 고유 ID (JWT subject로 사용)
     * @param role   사용자 역할 (예: "USER", "ADMIN")
     * @return 서명된 JWT Access Token 문자열
     */
    public String generateAccessToken(String userId, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiry);

        return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * Refresh Token을 생성한다.
     *
     * <p>Access Token 재발급 용도로 사용되며, type="refresh" 클레임으로
     * Access Token과 구분한다. 만료 시간이 Access Token보다 길다 (기본 7일).</p>
     *
     * @param userId 사용자 고유 ID (JWT subject로 사용)
     * @return 서명된 JWT Refresh Token 문자열
     */
    public String generateRefreshToken(String userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenExpiry);

        return Jwts.builder()
                .subject(userId)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * JWT 토큰의 유효성을 검증한다.
     *
     * <p>서명 검증과 만료 시간 확인을 수행한다.
     * 만료된 토큰이나 서명이 올바르지 않은 토큰은 false를 반환한다.</p>
     *
     * @param token 검증할 JWT 토큰 문자열
     * @return 유효하면 true, 만료/서명불일치/파싱오류 시 false
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT 토큰 만료: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.debug("JWT 토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * JWT 토큰에서 사용자 ID(subject)를 추출한다.
     *
     * @param token JWT 토큰 문자열
     * @return 토큰의 subject 클레임 (사용자 ID)
     * @throws JwtException 토큰 파싱 실패 시
     */
    public String getUserId(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * JWT 토큰에서 사용자 역할(role) 클레임을 추출한다.
     *
     * @param token JWT 토큰 문자열
     * @return role 클레임 값 (예: "USER", "ADMIN")
     * @throws JwtException 토큰 파싱 실패 시
     */
    public String getUserRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    /**
     * 해당 토큰이 Refresh Token인지 확인한다.
     *
     * <p>type 클레임이 "refresh"이면 Refresh Token으로 판별한다.</p>
     *
     * @param token JWT 토큰 문자열
     * @return Refresh Token이면 true, 그 외 false
     */
    public boolean isRefreshToken(String token) {
        try {
            String type = parseClaims(token).get("type", String.class);
            return "refresh".equals(type);
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * JWT 토큰을 1회 파싱하여 userId, role, refresh 여부를 한꺼번에 반환한다.
     *
     * <p>validateToken → isRefreshToken → getUserId → getUserRole 순으로
     * 개별 호출 시 매번 parseClaims()를 실행하는 비효율을 방지한다.
     * 이 메서드를 사용하면 토큰당 파싱이 1회로 줄어든다.</p>
     *
     * @param token JWT 토큰 문자열
     * @return 파싱 결과 (null이면 유효하지 않은 토큰)
     */
    public ParsedToken parse(String token) {
        try {
            Claims claims = parseClaims(token);
            return new ParsedToken(
                    claims.getSubject(),
                    claims.get("role", String.class),
                    "refresh".equals(claims.get("type", String.class))
            );
        } catch (ExpiredJwtException e) {
            log.debug("JWT 토큰 만료: {}", e.getMessage());
            return null;
        } catch (JwtException e) {
            log.debug("JWT 토큰 검증 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * JWT 토큰 1회 파싱 결과를 담는 불변 record.
     *
     * @param userId    사용자 고유 ID (JWT subject)
     * @param role      사용자 역할 (예: "USER", "ADMIN")
     * @param isRefresh Refresh Token 여부 (type="refresh" 클레임)
     */
    public record ParsedToken(String userId, String role, boolean isRefresh) {
    }

    /**
     * JWT 토큰의 클레임(Claims)을 파싱하여 반환한다.
     *
     * <p>내부적으로 서명 검증과 만료 확인을 함께 수행한다.</p>
     *
     * @param token JWT 토큰 문자열
     * @return 파싱된 Claims 객체
     * @throws JwtException 토큰 파싱/검증 실패 시
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
