package com.monglepick.monglepickbackend.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 엔티티
 *
 * <p>MySQL users 테이블과 매핑됩니다.
 * DDL 기준 PK는 user_id VARCHAR(50)입니다.</p>
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    /** 사용자 고유 식별자 (VARCHAR(50), DDL PK) */
    @Id
    @Column(name = "user_id", length = 50)
    private String userId;

    /** 이메일 주소 (로그인 ID로 사용) */
    @Column(unique = true, length = 255)
    private String email;

    /** 닉네임 (커뮤니티 표시명) */
    @Column(unique = true, length = 50)
    private String nickname;

    /** 비밀번호 해시 (BCrypt, 소셜 로그인 시 null) */
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    /** 프로필 이미지 URL */
    @Column(name = "profile_image", length = 500)
    private String profileImage;

    /** 로그인 제공자 */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Provider provider;

    /** 소셜 제공자 고유 ID */
    @Column(name = "provider_id", length = 200)
    private String providerId;

    /** 사용자 역할 (기본값: USER) */
    @Column(name = "user_role", length = 20)
    private String userRole;

    /** 생년월일 (YYYYMMDD) */
    @Column(name = "user_birth", length = 20)
    private String userBirth;

    /** 선택 약관 동의 */
    @Column(name = "option_term")
    private Boolean optionTerm;

    /** 필수 약관 동의 */
    @Column(name = "required_term")
    private Boolean requiredTerm;

    /** 계정 생성 시각 */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 정보 수정 시각 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 로그인 제공자 열거형 */
    public enum Provider {
        LOCAL, NAVER, KAKAO, GOOGLE
    }

    @Builder
    public User(String userId, String email, String nickname, String passwordHash,
                String profileImage, Provider provider, String providerId,
                String userRole, String userBirth, Boolean optionTerm, Boolean requiredTerm) {
        this.userId = userId;
        this.email = email;
        this.nickname = nickname;
        this.passwordHash = passwordHash;
        this.profileImage = profileImage;
        this.provider = provider != null ? provider : Provider.LOCAL;
        this.providerId = providerId;
        this.userRole = userRole != null ? userRole : "USER";
        this.userBirth = userBirth;
        this.optionTerm = optionTerm != null ? optionTerm : false;
        this.requiredTerm = requiredTerm != null ? requiredTerm : false;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /** 닉네임 변경 */
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    /** 프로필 이미지 변경 */
    public void updateProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    /** 비밀번호 변경 */
    public void updatePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
