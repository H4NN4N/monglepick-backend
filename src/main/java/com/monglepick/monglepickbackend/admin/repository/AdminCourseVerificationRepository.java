package com.monglepick.monglepickbackend.admin.repository;

import com.monglepick.monglepickbackend.domain.roadmap.entity.CourseVerification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AdminCourseVerificationRepository extends JpaRepository<CourseVerification, Long> {

    /**
     * 리뷰 인증 복합 필터 검색.
     *
     * userKeyword: 사용자 닉네임 또는 이메일 부분 일치 (EXISTS 서브쿼리 — 관계 FK 없이 User 조인)
     * courseTitleKeyword: 코스 제목 부분 일치 (EXISTS 서브쿼리 — RoadmapCourse 조인)
     */
    @Query(
        value =
        "SELECT v FROM CourseVerification v WHERE " +
        "v.verificationType = 'REVIEW' AND " +
        "(:reviewStatus IS NULL OR v.reviewStatus = :reviewStatus) AND " +
        "(:minConfidence IS NULL OR (v.aiConfidence IS NOT NULL AND v.aiConfidence >= :minConfidence)) AND " +
        "(:userKeyword IS NULL OR EXISTS (" +
        "  SELECT u FROM User u WHERE u.userId = v.userId AND (" +
        "    LOWER(u.nickname) LIKE LOWER(CONCAT('%', :userKeyword, '%')) OR " +
        "    LOWER(u.email)    LIKE LOWER(CONCAT('%', :userKeyword, '%'))))) AND " +
        "(:courseTitleKeyword IS NULL OR EXISTS (" +
        "  SELECT rc FROM RoadmapCourse rc WHERE rc.courseId = v.courseId AND " +
        "    LOWER(rc.title) LIKE LOWER(CONCAT('%', :courseTitleKeyword, '%')))) AND " +
        "(:fromDate IS NULL OR v.createdAt >= :fromDate) AND " +
        "(:toDate   IS NULL OR v.createdAt <  :toDate) " +
        "ORDER BY v.createdAt DESC",
        countQuery =
        "SELECT COUNT(v) FROM CourseVerification v WHERE " +
        "v.verificationType = 'REVIEW' AND " +
        "(:reviewStatus IS NULL OR v.reviewStatus = :reviewStatus) AND " +
        "(:minConfidence IS NULL OR (v.aiConfidence IS NOT NULL AND v.aiConfidence >= :minConfidence)) AND " +
        "(:userKeyword IS NULL OR EXISTS (" +
        "  SELECT u FROM User u WHERE u.userId = v.userId AND (" +
        "    LOWER(u.nickname) LIKE LOWER(CONCAT('%', :userKeyword, '%')) OR " +
        "    LOWER(u.email)    LIKE LOWER(CONCAT('%', :userKeyword, '%'))))) AND " +
        "(:courseTitleKeyword IS NULL OR EXISTS (" +
        "  SELECT rc FROM RoadmapCourse rc WHERE rc.courseId = v.courseId AND " +
        "    LOWER(rc.title) LIKE LOWER(CONCAT('%', :courseTitleKeyword, '%')))) AND " +
        "(:fromDate IS NULL OR v.createdAt >= :fromDate) AND " +
        "(:toDate   IS NULL OR v.createdAt <  :toDate)"
    )
    Page<CourseVerification> searchReviewVerifications(
            @Param("reviewStatus") String reviewStatus,
            @Param("minConfidence") Float minConfidence,
            @Param("userKeyword") String userKeyword,
            @Param("courseTitleKeyword") String courseTitleKeyword,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    @Query(
        "SELECT COUNT(v) FROM CourseVerification v WHERE " +
        "v.verificationType = 'REVIEW' AND v.reviewStatus = :reviewStatus"
    )
    long countReviewByStatus(@Param("reviewStatus") String reviewStatus);

    @Query(
        "SELECT cr.reviewText FROM CourseReview cr WHERE " +
        "cr.userId = :userId AND cr.courseId = :courseId AND cr.movieId = :movieId"
    )
    String findCourseReviewText(
            @Param("userId") String userId,
            @Param("courseId") String courseId,
            @Param("movieId") String movieId
    );

    @Query("SELECT u.nickname FROM User u WHERE u.userId = :userId")
    String findUserNicknameByUserId(@Param("userId") String userId);

    @Query("SELECT rc.title FROM RoadmapCourse rc WHERE rc.courseId = :courseId")
    String findCourseTitleByCourseId(@Param("courseId") String courseId);
}
