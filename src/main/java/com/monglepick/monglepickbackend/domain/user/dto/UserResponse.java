package com.monglepick.monglepickbackend.domain.user.dto;

import com.monglepick.monglepickbackend.domain.user.entity.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 사용자 정보 응답 DTO
 *
 * <p>프로필 조회 시 반환되는 사용자 정보입니다.
 * 비밀번호 등 민감한 정보는 포함하지 않습니다.</p>
 *
 * @param userId 사용자 ID
 * @param email 이메일 주소
 * @param nickname 닉네임
 * @param profileImage 프로필 이미지 URL (null 가능)
 * @param createdAt 가입일 (yyyy-MM-dd 형식)
 */
public record UserResponse(
        String userId,
        String email,
        String nickname,
        String profileImage,
        String createdAt
) {
    /** 가입일 포맷 — "2026-03-24" 형태 */
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * User 엔티티로부터 UserResponse를 생성하는 팩토리 메서드
     *
     * @param user User 엔티티
     * @return UserResponse 인스턴스
     */
    public static UserResponse from(User user) {
        // BaseTimeEntity.createdAt(LocalDateTime)을 yyyy-MM-dd 문자열로 변환
        String formattedDate = null;
        LocalDateTime created = user.getCreatedAt();
        if (created != null) {
            formattedDate = created.format(DATE_FORMAT);
        }

        return new UserResponse(
                user.getUserId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImage(),
                formattedDate
        );
    }
}
