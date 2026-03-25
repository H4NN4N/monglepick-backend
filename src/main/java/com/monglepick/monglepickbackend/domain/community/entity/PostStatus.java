package com.monglepick.monglepickbackend.domain.community.entity;

/**
 * 게시글 상태 열거형.
 *
 * <p>게시글의 임시저장/게시 상태를 구분한다.</p>
 */
public enum PostStatus {
    /** 임시저장 상태 (작성자만 조회 가능) */
    DRAFT,
    /** 게시 완료 상태 (전체 공개) */
    PUBLISHED
}
