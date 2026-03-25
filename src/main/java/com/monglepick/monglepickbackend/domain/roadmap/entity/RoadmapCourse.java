package com.monglepick.monglepickbackend.domain.roadmap.entity;

/* BaseAuditEntity로 변경 — created_at/updated_at에 더해 created_by/updated_by 자동 관리 */
import com.monglepick.monglepickbackend.global.entity.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 도장깨기 코스 엔티티 — roadmap_courses 테이블 매핑.
 *
 * <p>AI Agent가 생성한 영화 도장깨기(로드맵) 코스를 저장한다.
 * 각 코스는 테마별로 구성된 영화 목록과 난이도, 퀴즈 활성화 여부를 포함한다.</p>
 *
 * <h3>주요 필드</h3>
 * <ul>
 *   <li>{@code courseId} — 코스 고유 식별자 (UNIQUE, 예: "nolan-filmography")</li>
 *   <li>{@code title} — 코스 제목 (예: "크리스토퍼 놀란 필모그래피 정복")</li>
 *   <li>{@code theme} — 테마/카테고리 (예: "감독별", "장르별", "시대별")</li>
 *   <li>{@code movieIds} — 코스에 포함된 영화 ID 목록 (JSON 배열, 필수)</li>
 *   <li>{@code movieCount} — 코스 내 영화 수 (필수)</li>
 *   <li>{@code difficulty} — 난이도 (beginner, intermediate, advanced)</li>
 *   <li>{@code quizEnabled} — 퀴즈 활성화 여부</li>
 * </ul>
 *
 * <h3>변경 이력</h3>
 * <ul>
 *   <li>PK 필드명: id → roadmapCourseId (컬럼명: roadmap_course_id)</li>
 *   <li>BaseTimeEntity → BaseAuditEntity로 변경 (created_by/updated_by 추가)</li>
 *   <li>수동 createdBy 필드 제거 — BaseAuditEntity에서 상속 (AuditorAware 자동 주입)</li>
 * </ul>
 */
@Entity
@Table(name = "roadmap_courses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RoadmapCourse extends BaseAuditEntity {

    /**
     * 코스 고유 ID (BIGINT AUTO_INCREMENT PK).
     * 필드명 변경: id → roadmapCourseId (엔티티 PK 네이밍 통일)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "roadmap_course_id")
    private Long roadmapCourseId;

    /**
     * 코스 고유 식별자 (UNIQUE).
     * 사람이 읽을 수 있는 slug 형태의 ID.
     * 예: "nolan-filmography", "90s-classics", "korean-thrillers"
     */
    @Column(name = "course_id", length = 50, nullable = false, unique = true)
    private String courseId;

    /** 코스 제목 (필수, 최대 300자) */
    @Column(name = "title", length = 300, nullable = false)
    private String title;

    /** 코스 설명 (선택, TEXT 타입) */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 테마/카테고리 (최대 100자).
     * 예: "감독별", "장르별", "시대별", "국가별"
     */
    @Column(name = "theme", length = 100)
    private String theme;

    /**
     * 코스에 포함된 영화 ID 목록 (JSON 배열, 필수).
     * 예: ["12345", "67890", "11111"]
     * 순서가 곧 시청 순서를 의미한다.
     */
    @Column(name = "movie_ids", columnDefinition = "json", nullable = false)
    private String movieIds;

    /** 코스 내 영화 수 (필수) */
    @Column(name = "movie_count", nullable = false)
    private Integer movieCount;

    /**
     * 난이도.
     * ENUM('beginner', 'intermediate', 'advanced') 중 하나.
     * 기본값: beginner
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty")
    @Builder.Default
    private Difficulty difficulty = Difficulty.beginner;

    /**
     * 퀴즈 활성화 여부.
     * true이면 코스 내 영화별 퀴즈가 제공된다.
     * 기본값: false
     */
    @Column(name = "quiz_enabled")
    @Builder.Default
    private Boolean quizEnabled = false;

    /* createdBy 수동 필드 제거 — BaseAuditEntity에서 @CreatedBy로 자동 관리 */
    /* created_at, updated_at → BaseTimeEntity에서 상속 */
    /* created_by, updated_by → BaseAuditEntity에서 상속 */

    /**
     * 코스 난이도 열거형.
     *
     * <p>MySQL ENUM('beginner','intermediate','advanced')에 매핑된다.</p>
     */
    public enum Difficulty {
        /** 초급 — 입문자를 위한 대중적인 영화 위주 */
        beginner,
        /** 중급 — 어느 정도 영화 경험이 있는 사용자용 */
        intermediate,
        /** 고급 — 영화 마니아를 위한 깊이 있는 코스 */
        advanced
    }
}
