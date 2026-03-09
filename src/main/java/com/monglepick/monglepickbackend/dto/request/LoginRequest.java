package com.monglepick.monglepickbackend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 요청 DTO
 *
 * <p>POST /api/v1/auth/login 요청 본문에 사용됩니다.</p>
 *
 * @param email 이메일 주소 (필수)
 * @param password 비밀번호 (필수)
 */
public record LoginRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {
}
