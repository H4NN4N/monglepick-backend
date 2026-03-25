package com.monglepick.monglepickbackend.domain.user.entity;

import com.monglepick.monglepickbackend.global.constants.UserRole;
import com.monglepick.monglepickbackend.global.entity.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
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
/**
 * BaseAuditEntity 상속: created_at, updated_at, created_by, updated_by 자동 관리
 * — 수동 createdAt/updatedAt 필드 및 @PrePersist/@PreUpdate 메서드 제거됨
 */
public class User extends BaseAuditEntity {

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
    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", length = 20)
    private UserRole userRole;

    /** 생년월일 (YYYYMMDD) */
    @Column(name = "user_birth", length = 20)
    private String userBirth;

    /** 선택 약관 동의 */
    @Column(name = "option_term")
    private Boolean optionTerm;

    /** 필수 약관 동의 */
    @Column(name = "required_term")
    private Boolean requiredTerm;

    /* created_at, updated_at은 BaseAuditEntity(→BaseTimeEntity)에서 자동 관리 — 수동 필드 제거됨 */

    /** 로그인 제공자 열거형 */
    public enum Provider {
        LOCAL, NAVER, KAKAO, GOOGLE
    }

    @Builder
    public User(String userId, String email, String nickname, String passwordHash,
                String profileImage, Provider provider, String providerId,
                UserRole userRole, String userBirth, Boolean optionTerm, Boolean requiredTerm) {
        this.userId = userId;
        this.email = email;
        this.nickname = nickname;
        this.passwordHash = passwordHash;
        this.profileImage = profileImage;
        this.provider = provider != null ? provider : Provider.LOCAL;
        this.providerId = providerId;
        this.userRole = userRole != null ? userRole : UserRole.USER;
        this.userBirth = userBirth;
        this.optionTerm = optionTerm != null ? optionTerm : false;
        this.requiredTerm = requiredTerm != null ? requiredTerm : false;
    }

    /* @PrePersist/@PreUpdate 제거됨 — BaseTimeEntity의 @CreationTimestamp/@UpdateTimestamp로 자동 관리 */

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
