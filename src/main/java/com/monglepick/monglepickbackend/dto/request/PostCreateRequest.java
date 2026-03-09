package com.monglepick.monglepickbackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 게시글 작성 요청 DTO
 *
 * <p>POST /api/v1/posts 요청 본문에 사용됩니다.</p>
 *
 * @param title 게시글 제목 (필수, 1~200자)
 * @param content 게시글 본문 (필수)
 * @param category 게시글 카테고리 (필수: FREE, DISCUSSION, RECOMMENDATION, NEWS)
 */
public record PostCreateRequest(
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
        String title,

        @NotBlank(message = "내용은 필수입니다.")
        String content,

        @NotNull(message = "카테고리는 필수입니다.")
        String category
) {
}
