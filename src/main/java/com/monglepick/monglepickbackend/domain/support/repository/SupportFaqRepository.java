package com.monglepick.monglepickbackend.domain.support.repository;

import com.monglepick.monglepickbackend.domain.support.entity.SupportCategory;
import com.monglepick.monglepickbackend.domain.support.entity.SupportFaq;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * FAQ JPA 레포지토리.
 *
 * <p>{@link SupportFaq} 엔티티에 대한 데이터 접근 계층.
 * 카테고리별 조회와 전체 목록 조회를 제공한다.</p>
 */
public interface SupportFaqRepository extends JpaRepository<SupportFaq, Long> {

    /**
     * 특정 카테고리의 FAQ 목록을 정렬 조건에 따라 조회한다.
     *
     * <p>사용 예: 카테고리 탭 선택 시 해당 카테고리의 FAQ를 최신순 또는
     * 도움됨 순으로 정렬하여 조회한다.</p>
     *
     * <pre>{@code
     * // 최신순 조회
     * faqRepository.findByCategory(SupportCategory.PAYMENT, Sort.by(Sort.Direction.DESC, "createdAt"));
     *
     * // 도움됨 많은 순 조회
     * faqRepository.findByCategory(SupportCategory.ACCOUNT, Sort.by(Sort.Direction.DESC, "helpfulCount"));
     * }</pre>
     *
     * @param category 조회할 카테고리
     * @param sort     정렬 조건 (예: Sort.by(DESC, "createdAt"))
     * @return 해당 카테고리 FAQ 목록 (없으면 빈 리스트)
     */
    List<SupportFaq> findByCategory(SupportCategory category, Sort sort);

    /**
     * 전체 FAQ 목록을 생성일 내림차순(최신순)으로 조회한다.
     *
     * <p>관리자 FAQ 관리 화면 또는 "전체" 탭 선택 시 사용한다.</p>
     *
     * @return 전체 FAQ 목록 (최신 등록순)
     */
    List<SupportFaq> findAllByOrderByCreatedAtDesc();
}
