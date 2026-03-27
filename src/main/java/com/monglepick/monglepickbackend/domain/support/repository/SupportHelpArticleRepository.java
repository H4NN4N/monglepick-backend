package com.monglepick.monglepickbackend.domain.support.repository;

import com.monglepick.monglepickbackend.domain.support.entity.SupportCategory;
import com.monglepick.monglepickbackend.domain.support.entity.SupportHelpArticle;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 도움말 문서 JPA 레포지토리.
 *
 * <p>{@link SupportHelpArticle} 엔티티에 대한 데이터 접근 계층.
 * 카테고리별 조회와 조회수 직접 증가 쿼리를 제공한다.</p>
 */
public interface SupportHelpArticleRepository extends JpaRepository<SupportHelpArticle, Long> {

    /**
     * 특정 카테고리의 도움말 문서 목록을 정렬 조건에 따라 조회한다.
     *
     * <p>사용 예: 카테고리 탭 선택 시 해당 카테고리의 도움말 문서를
     * 최신순 또는 조회수 순으로 정렬하여 조회한다.</p>
     *
     * <pre>{@code
     * // 최신순 조회
     * articleRepository.findByCategory(SupportCategory.CHAT, Sort.by(Sort.Direction.DESC, "createdAt"));
     *
     * // 조회수 많은 순 조회
     * articleRepository.findByCategory(SupportCategory.CHAT, Sort.by(Sort.Direction.DESC, "viewCount"));
     * }</pre>
     *
     * @param category 조회할 카테고리
     * @param sort     정렬 조건
     * @return 해당 카테고리 도움말 문서 목록 (없으면 빈 리스트)
     */
    List<SupportHelpArticle> findByCategory(SupportCategory category, Sort sort);

    /**
     * 도움말 문서 조회수를 DB에서 직접 1 증가시킨다.
     *
     * <p>엔티티 전체를 재로딩하지 않고 UPDATE 쿼리만 실행하므로,
     * 조회수 증가로 인한 불필요한 SELECT + dirty-checking 비용을 피한다.</p>
     *
     * <p>@Modifying: 이 쿼리가 데이터를 변경함을 Spring Data JPA에 알린다.
     * clearAutomatically = true: 쿼리 실행 후 1차 캐시를 초기화하여
     * 이후 동일 엔티티 조회 시 DB에서 최신 view_count를 반영한다.</p>
     *
     * <p>호출부(@Transactional 필수): 이 메서드를 호출하는 서비스 메서드에는
     * 반드시 {@code @Transactional}을 선언해야 한다.</p>
     *
     * @param articleId 조회수를 증가시킬 도움말 문서 ID
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE SupportHelpArticle a SET a.viewCount = a.viewCount + 1 WHERE a.articleId = :articleId")
    void incrementViewCount(@Param("articleId") Long articleId);
}
