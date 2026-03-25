package com.monglepick.monglepickbackend.domain.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

/**
 * Refresh Token 화이트리스트 엔티티.
 *
 * <p>DB에 저장된 Refresh Token만 유효하게 처리하여
 * 토큰 탈취 시 재사용을 방지한다 (Refresh Token Rotation).</p>
 *
 * <p>KMG 프로젝트의 jwt_refresh_entity 테이블과 동일한 구조이며,
 * useremail 대신 userId를 저장한다.</p>
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshEntity {

    /** 자동 증가 PK */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 토큰 소유자 userId (users.user_id FK 개념) */
    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    /** Refresh Token JWT 문자열 (최대 512자) */
    @Column(name = "refresh_token", nullable = false, length = 512)
    private String refreshToken;

    /** 토큰 발급 시각 */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public RefreshEntity(String userId, String refreshToken) {
        this.userId = userId;
        this.refreshToken = refreshToken;
        this.createdAt = LocalDateTime.now();
    }
}
