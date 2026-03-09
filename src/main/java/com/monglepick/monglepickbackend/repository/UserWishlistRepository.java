package com.monglepick.monglepickbackend.repository;

import com.monglepick.monglepickbackend.entity.UserWishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 위시리스트 JPA 리포지토리
 *
 * <p>MySQL user_wishlist 테이블에 대한 데이터 접근 레이어입니다.
 * 사용자의 보고싶은 영화 목록을 관리합니다.</p>
 */
@Repository
public interface UserWishlistRepository extends JpaRepository<UserWishlist, Long> {

    /**
     * 특정 사용자의 위시리스트를 페이징으로 조회합니다.
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 페이지 단위의 위시리스트
     */
    Page<UserWishlist> findByUserId(Long userId, Pageable pageable);

    /**
     * 특정 사용자가 특정 영화를 위시리스트에 추가했는지 확인합니다.
     * <p>중복 추가 방지를 위한 검증에 사용됩니다.</p>
     *
     * @param userId 사용자 ID
     * @param movieId 영화 ID
     * @return 이미 추가되어 있으면 true
     */
    boolean existsByUserIdAndMovieId(Long userId, Long movieId);

    /**
     * 특정 사용자의 특정 영화 위시리스트 항목을 조회합니다.
     * <p>위시리스트에서 삭제할 때 사용됩니다.</p>
     *
     * @param userId 사용자 ID
     * @param movieId 영화 ID
     * @return 위시리스트 항목 Optional
     */
    Optional<UserWishlist> findByUserIdAndMovieId(Long userId, Long movieId);
}
