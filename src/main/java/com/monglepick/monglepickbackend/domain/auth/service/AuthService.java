package com.monglepick.monglepickbackend.domain.auth.service;

import com.monglepick.monglepickbackend.domain.auth.dto.AuthDto.AuthResponse;
import com.monglepick.monglepickbackend.domain.auth.dto.AuthDto.LoginRequest;
import com.monglepick.monglepickbackend.domain.auth.dto.AuthDto.OAuthRequest;
import com.monglepick.monglepickbackend.domain.auth.dto.AuthDto.RefreshRequest;
import com.monglepick.monglepickbackend.domain.auth.dto.AuthDto.SignupRequest;
import com.monglepick.monglepickbackend.domain.auth.dto.AuthDto.TokenResponse;
import com.monglepick.monglepickbackend.domain.auth.dto.AuthDto.UserInfo;
import com.monglepick.monglepickbackend.domain.auth.service.OAuthProviderClient.OAuthUserInfo;
import com.monglepick.monglepickbackend.domain.user.entity.User;
import com.monglepick.monglepickbackend.domain.user.repository.UserRepository;
import com.monglepick.monglepickbackend.global.exception.BusinessException;
import com.monglepick.monglepickbackend.global.exception.ErrorCode;
import com.monglepick.monglepickbackend.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 인증 서비스 — 회원가입, 로그인, 소셜 로그인, 토큰 갱신 비즈니스 로직.
 *
 * <p>로컬 회원가입(이메일+비밀번호)과 소셜 로그인(Google/Kakao/Naver)을
 * 모두 지원하며, JWT 기반의 Access Token + Refresh Token을 발급한다.</p>
 *
 * <h3>인증 흐름</h3>
 * <ul>
 *   <li><b>로컬 회원가입</b>: 이메일/닉네임 중복 확인 → BCrypt 해싱 → 사용자 생성 → JWT 발급</li>
 *   <li><b>로컬 로그인</b>: 이메일로 사용자 조회 → 비밀번호 검증 → JWT 발급</li>
 *   <li><b>소셜 로그인</b>: 인가 코드 → 토큰 교환 → 사용자 정보 조회 → 기존/신규 판별 → JWT 발급</li>
 *   <li><b>토큰 갱신</b>: Refresh Token 검증 → 새 토큰 쌍 발급</li>
 * </ul>
 *
 * <h3>의존성</h3>
 * <ul>
 *   <li>{@link UserRepository} — 사용자 CRUD</li>
 *   <li>{@link PasswordEncoder} — BCrypt 비밀번호 해싱/검증</li>
 *   <li>{@link JwtTokenProvider} — JWT 토큰 생성/검증</li>
 *   <li>{@link OAuthProviderClient} — 소셜 로그인 토큰 교환 및 사용자 정보 조회</li>
 * </ul>
 *
 * @see com.monglepick.monglepickbackend.domain.auth.controller.AuthController
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    /** 사용자 리포지토리 (사용자 조회/저장) */
    private final UserRepository userRepository;

    /** 비밀번호 암호화/검증 (BCrypt) */
    private final PasswordEncoder passwordEncoder;

    /** JWT 토큰 생성/검증 프로바이더 */
    private final JwtTokenProvider jwtTokenProvider;

    /** OAuth 소셜 로그인 제공자 클라이언트 */
    private final OAuthProviderClient oauthProviderClient;

    /**
     * 로컬 회원가입을 처리한다.
     *
     * <p>이메일과 닉네임의 중복 여부를 확인한 후,
     * 비밀번호를 BCrypt로 해싱하여 사용자를 생성하고 JWT를 발급한다.</p>
     *
     * <h4>처리 순서</h4>
     * <ol>
     *   <li>이메일 중복 확인 → EMAIL_ALREADY_EXISTS 예외</li>
     *   <li>닉네임 중복 확인 → NICKNAME_ALREADY_EXISTS 예외</li>
     *   <li>비밀번호 BCrypt 해싱</li>
     *   <li>UUID 기반 사용자 ID 생성</li>
     *   <li>User 엔티티 빌드 및 저장</li>
     *   <li>JWT Access Token + Refresh Token 발급</li>
     * </ol>
     *
     * @param request 회원가입 요청 (이메일, 비밀번호, 닉네임)
     * @return 인증 응답 (토큰 쌍 + 사용자 정보)
     * @throws BusinessException 이메일 중복(A001) 또는 닉네임 중복(A002) 시
     */
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        log.info("로컬 회원가입 요청 — email: {}", request.email());

        // ── 1단계: 이메일 중복 확인 ──
        if (userRepository.existsByEmail(request.email())) {
            log.warn("회원가입 실패 — 이미 사용 중인 이메일: {}", request.email());
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // ── 2단계: 닉네임 중복 확인 ──
        if (userRepository.existsByNickname(request.nickname())) {
            log.warn("회원가입 실패 — 이미 사용 중인 닉네임: {}", request.nickname());
            throw new BusinessException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        // ── 3단계: 비밀번호 BCrypt 해싱 ──
        String passwordHash = passwordEncoder.encode(request.password());

        // ── 4단계: UUID 기반 사용자 ID 생성 ──
        String userId = UUID.randomUUID().toString();

        // ── 5단계: User 엔티티 빌드 및 저장 ──
        User user = User.builder()
                .userId(userId)
                .email(request.email())
                .nickname(request.nickname())
                .passwordHash(passwordHash)
                .provider(User.Provider.LOCAL)
                .requiredTerm(true)
                .build();

        userRepository.save(user);
        log.info("로컬 회원가입 완료 — userId: {}, email: {}", userId, request.email());

        // ── 6단계: JWT 발급 및 응답 빌드 ──
        return buildAuthResponse(user);
    }

    /**
     * 로컬 로그인을 처리한다.
     *
     * <p>이메일로 사용자를 조회하고, 비밀번호를 검증한 후 JWT를 발급한다.
     * 소셜 로그인으로 가입된 이메일로 로컬 로그인을 시도하면
     * SOCIAL_EMAIL_EXISTS 예외가 발생한다.</p>
     *
     * @param request 로그인 요청 (이메일, 비밀번호)
     * @return 인증 응답 (토큰 쌍 + 사용자 정보)
     * @throws BusinessException 사용자 미존재(A003), 소셜 가입 이메일(A007), 비밀번호 불일치(A003) 시
     */
    public AuthResponse login(LoginRequest request) {
        log.info("로컬 로그인 요청 — email: {}", request.email());

        // ── 1단계: 이메일로 사용자 조회 ──
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("로그인 실패 — 존재하지 않는 이메일: {}", request.email());
                    return new BusinessException(ErrorCode.INVALID_CREDENTIALS);
                });

        // ── 2단계: 소셜 로그인 사용자인지 확인 ──
        if (user.getProvider() != User.Provider.LOCAL) {
            log.warn("로그인 실패 — 소셜 로그인 이메일로 로컬 로그인 시도: {} (provider: {})",
                    request.email(), user.getProvider());
            throw new BusinessException(ErrorCode.SOCIAL_EMAIL_EXISTS);
        }

        // ── 3단계: 비밀번호 검증 ──
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("로그인 실패 — 비밀번호 불일치: {}", request.email());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        log.info("로컬 로그인 성공 — userId: {}", user.getUserId());
        return buildAuthResponse(user);
    }

    /**
     * 소셜 로그인(OAuth)을 처리한다.
     *
     * <p>인가 코드를 소셜 제공자의 Access Token으로 교환하고,
     * 사용자 프로필을 조회하여 기존/신규 사용자를 판별한 후 JWT를 발급한다.</p>
     *
     * <h4>처리 순서</h4>
     * <ol>
     *   <li>providerName을 Provider 열거형으로 변환</li>
     *   <li>인가 코드 → 소셜 Access Token 교환</li>
     *   <li>소셜 Access Token → 사용자 프로필 조회</li>
     *   <li>provider+providerId로 기존 사용자 조회 → 있으면 로그인 처리</li>
     *   <li>이메일로 기존 사용자 조회 → 있으면 다른 제공자로 가입된 이메일이므로 예외</li>
     *   <li>완전 신규 → 사용자 생성 후 JWT 발급</li>
     * </ol>
     *
     * @param providerName 소셜 로그인 제공자 이름 (google, kakao, naver)
     * @param request      소셜 로그인 요청 (인가 코드, 리다이렉트 URI)
     * @return 인증 응답 (토큰 쌍 + 사용자 정보)
     * @throws BusinessException OAuth 실패(A006), 소셜 이메일 충돌(A007) 시
     */
    @Transactional
    public AuthResponse oauthLogin(String providerName, OAuthRequest request) {
        log.info("소셜 로그인 요청 — provider: {}", providerName);

        // ── 1단계: providerName을 Provider 열거형으로 변환 ──
        User.Provider provider;
        try {
            provider = User.Provider.valueOf(providerName.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("소셜 로그인 실패 — 지원하지 않는 제공자: {}", providerName);
            throw new BusinessException(ErrorCode.OAUTH_FAILED);
        }

        // LOCAL은 소셜 로그인 제공자가 아님
        if (provider == User.Provider.LOCAL) {
            log.warn("소셜 로그인 실패 — LOCAL은 소셜 제공자가 아님");
            throw new BusinessException(ErrorCode.OAUTH_FAILED);
        }

        // ── 2단계: 인가 코드 → 소셜 Access Token 교환 ──
        String oauthAccessToken = oauthProviderClient.exchangeCode(
                provider, request.code(), request.redirectUri()
        );

        // ── 3단계: 소셜 Access Token → 사용자 프로필 조회 ──
        OAuthUserInfo userInfo = oauthProviderClient.fetchUserInfo(provider, oauthAccessToken);

        // ── 4단계: provider+providerId로 기존 사용자 조회 ──
        return userRepository.findByProviderAndProviderId(provider, userInfo.providerId())
                .map(existingUser -> {
                    log.info("소셜 로그인 — 기존 사용자 발견: userId={}", existingUser.getUserId());
                    return buildAuthResponse(existingUser);
                })
                .orElseGet(() -> handleNewOAuthUser(provider, userInfo));
    }

    /**
     * 토큰 갱신을 처리한다.
     *
     * <p>Refresh Token의 유효성을 검증하고, 해당 사용자의 새로운
     * Access Token + Refresh Token 쌍을 발급한다.</p>
     *
     * @param request 토큰 갱신 요청 (리프레시 토큰)
     * @return 새로운 토큰 쌍 (Access Token + Refresh Token)
     * @throws BusinessException 토큰 유효하지 않음(A004), 사용자 미존재(A004) 시
     */
    @Transactional(readOnly = true)
    public TokenResponse refreshToken(RefreshRequest request) {
        String refreshToken = request.refreshToken();

        // ── 1단계: Refresh Token 1회 파싱 (검증 + 타입 확인 + userId 추출 동시 수행) ──
        JwtTokenProvider.ParsedToken parsed = jwtTokenProvider.parse(refreshToken);

        if (parsed == null) {
            log.warn("토큰 갱신 실패 — 유효하지 않은 리프레시 토큰");
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        // ── 2단계: Refresh Token 타입 확인 ──
        if (!parsed.isRefresh()) {
            log.warn("토큰 갱신 실패 — 리프레시 토큰이 아닌 토큰으로 갱신 시도");
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        // ── 3단계: 사용자 존재 확인 ──
        String userId = parsed.userId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("토큰 갱신 실패 — 사용자를 찾을 수 없음: userId={}", userId);
                    return new BusinessException(ErrorCode.INVALID_TOKEN);
                });

        // ── 4단계: 새 토큰 쌍 생성 ──
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getUserId(), user.getUserRole());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId());

        log.info("토큰 갱신 완료 — userId: {}", userId);
        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    // ──────────────────────────────────────────────
    // Private 헬퍼 메서드
    // ──────────────────────────────────────────────

    /**
     * 신규 OAuth 사용자를 처리한다.
     *
     * <p>이메일로 기존 사용자를 검색하여 다른 제공자로 가입되어 있으면
     * 충돌 예외를 발생시키고, 완전 신규인 경우 사용자를 생성한다.</p>
     *
     * @param provider 소셜 로그인 제공자
     * @param userInfo 소셜 제공자로부터 받은 사용자 정보
     * @return 인증 응답 (토큰 쌍 + 사용자 정보)
     * @throws BusinessException 다른 제공자로 이미 가입된 이메일(A007) 시
     */
    private AuthResponse handleNewOAuthUser(User.Provider provider, OAuthUserInfo userInfo) {
        // ── 이메일로 기존 사용자 확인 (다른 제공자로 가입 여부) ──
        if (userInfo.email() != null && userRepository.existsByEmail(userInfo.email())) {
            log.warn("소셜 로그인 실패 — 다른 제공자로 이미 가입된 이메일: {} (시도: {})",
                    userInfo.email(), provider);
            throw new BusinessException(ErrorCode.SOCIAL_EMAIL_EXISTS);
        }

        // ── 신규 사용자 생성 ──
        String userId = UUID.randomUUID().toString();

        User newUser = User.builder()
                .userId(userId)
                .email(userInfo.email())
                .nickname(userInfo.nickname())
                .profileImage(userInfo.profileImage())
                .provider(provider)
                .providerId(userInfo.providerId())
                .requiredTerm(true)
                .build();

        userRepository.save(newUser);
        log.info("소셜 로그인 — 신규 사용자 생성: userId={}, provider={}", userId, provider);

        return buildAuthResponse(newUser);
    }

    /**
     * User 엔티티로부터 인증 응답(AuthResponse)을 빌드한다.
     *
     * <p>JWT Access Token + Refresh Token 쌍을 생성하고,
     * 사용자 요약 정보(UserInfo)와 함께 응답 객체를 구성한다.</p>
     *
     * @param user 사용자 엔티티
     * @return 인증 응답 (토큰 쌍 + 사용자 정보)
     */
    private AuthResponse buildAuthResponse(User user) {
        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUserId(), user.getUserRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId());

        // 사용자 요약 정보 빌드
        UserInfo userInfo = new UserInfo(
                user.getUserId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImage(),
                user.getProvider().name(),
                user.getUserRole()
        );

        return new AuthResponse(accessToken, refreshToken, userInfo);
    }
}
