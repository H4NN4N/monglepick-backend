package com.monglepick.monglepickbackend.domain.user.repository;

import com.monglepick.monglepickbackend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 사용자 리포지토리.
 *
 * <p>users 테이블에 대한 기본 CRUD를 지원한다.
 * PK는 user_id (VARCHAR(50))이므로 JpaRepository의 ID 타입은 String이다.</p>
 *
 * <h3>인증 관련 쿼리 메서드</h3>
 * <ul>
 *   <li>{@link #findByEmail(String)} — 이메일로 사용자 조회 (로그인 시 사용)</li>
 *   <li>{@link #findByProviderAndProviderId(User.Provider, String)} — 소셜 로그인 제공자+ID로 조회</li>
 *   <li>{@link #existsByEmail(String)} — 이메일 중복 확인 (회원가입 시 사용)</li>
 *   <li>{@link #existsByNickname(String)} — 닉네임 중복 확인 (회원가입 시 사용)</li>
 * </ul>
 */
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * 이메일로 사용자를 조회한다.
     *
     * <p>로컬 로그인 시 이메일+비밀번호 검증과
     * 소셜 로그인 시 이메일 중복 확인에 사용된다.</p>
     *
     * @param email 조회할 이메일 주소
     * @return 해당 이메일의 사용자 (없으면 빈 Optional)
     */
    Optional<User> findByEmail(String email);

    /**
     * 소셜 로그인 제공자와 제공자 고유 ID로 사용자를 조회한다.
     *
     * <p>소셜 로그인 시 기존 가입 여부를 확인할 때 사용된다.
     * 동일한 소셜 제공자+ID 조합이면 기존 사용자로 판별한다.</p>
     *
     * @param provider   소셜 로그인 제공자 (GOOGLE, KAKAO, NAVER)
     * @param providerId 제공자가 발급한 사용자 고유 ID
     * @return 해당 소셜 계정의 사용자 (없으면 빈 Optional)
     */
    Optional<User> findByProviderAndProviderId(User.Provider provider, String providerId);

    /**
     * 해당 이메일이 이미 존재하는지 확인한다.
     *
     * <p>회원가입 시 이메일 중복 검사에 사용된다.
     * COUNT 쿼리 대신 EXISTS 서브쿼리로 최적화된다.</p>
     *
     * @param email 확인할 이메일 주소
     * @return 존재하면 true, 없으면 false
     */
    boolean existsByEmail(String email);

    /**
     * 해당 닉네임이 이미 존재하는지 확인한다.
     *
     * <p>회원가입 시 닉네임 중복 검사에 사용된다.</p>
     *
     * @param nickname 확인할 닉네임
     * @return 존재하면 true, 없으면 false
     */
    boolean existsByNickname(String nickname);
}
