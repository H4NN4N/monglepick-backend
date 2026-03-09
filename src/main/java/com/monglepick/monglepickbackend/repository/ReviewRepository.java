package com.monglepick.monglepickbackend.repository;

import com.monglepick.monglepickbackend.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 리뷰 JPA 리포지토리
 *
 * <p>MySQL reviews 테이블에 대한 데이터 접근 레이어입니다.
 * 영화별 리뷰 조회, 사용자별 리뷰 조회, 중복 리뷰 검사를 지원합니다.</p>
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * 특정 영화의 리뷰 목록을 조회합니다.
     *
     * @param movieId 영화 ID
     * @return 해당 영화의 리뷰 목록
     */
    List<Review> findByMovieId(Long movieId);

    /**
     * 특정 사용자의 리뷰 목록을 페이징으로 조회합니다.
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 페이지 단위의 리뷰 목록
     */
    Page<Review> findByUserId(Long userId, Pageable pageable);

    /**
     * 특정 사용자가 특정 영화에 이미 리뷰를 작성했는지 확인합니다.
     * <p>중복 리뷰 방지를 위한 검증에 사용됩니다.</p>
     *
     * @param userId 사용자 ID
     * @param movieId 영화 ID
     * @return 이미 리뷰가 존재하면 true
     */
    boolean existsByUserIdAndMovieId(Long userId, Long movieId);
}
