package com.monglepick.monglepickbackend.domain.auth.service;

import com.monglepick.monglepickbackend.domain.auth.dto.LoginRequest;
import com.monglepick.monglepickbackend.domain.auth.dto.SignUpRequest;
import com.monglepick.monglepickbackend.domain.auth.dto.TokenResponse;
import com.monglepick.monglepickbackend.domain.user.dto.UserResponse;
import com.monglepick.monglepickbackend.domain.user.entity.User;
import com.monglepick.monglepickbackend.global.exception.BusinessException;
import com.monglepick.monglepickbackend.global.exception.ErrorCode;
import com.monglepick.monglepickbackend.domain.user.repository.UserRepository;
import com.monglepick.monglepickbackend.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 인증 서비스
 *
 * <p>회원가입, 로그인, 토큰 갱신 등 인증 관련 비즈니스 로직을 처리합니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 회원가입을 처리합니다.
     *
     * @param request 회원가입 요청 (이메일, 비밀번호, 닉네임)
     * @return JWT 토큰 쌍 (액세스 + 리프레시)
     * @throws BusinessException 이메일/닉네임 중복 시
     */
    @Transactional
    public TokenResponse signUp(SignUpRequest request) {
        // 1. 이메일 중복 검사
        if (userRepository.existsByEmail(request.email())) {
            log.warn("회원가입 실패 - 이메일 중복: {}", request.email());
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 2. 닉네임 중복 검사
        if (userRepository.existsByNickname(request.nickname())) {
            log.warn("회원가입 실패 - 닉네임 중복: {}", request.nickname());
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        // 3. 사용자 엔티티 생성 (UUID 기반 userId, 비밀번호 BCrypt 암호화)
        User user = User.builder()
                .userId(UUID.randomUUID().toString())
                .email(request.email())
                .nickname(request.nickname())
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();

        // 4. DB에 사용자 저장
        User savedUser = userRepository.save(user);
        log.info("회원가입 성공 - userId: {}, email: {}", savedUser.getUserId(), savedUser.getEmail());

        // 5. JWT 토큰 발급 (가입 즉시 로그인 처리)
        return generateTokens(savedUser.getUserId());
    }

    /**
     * 로그인을 처리합니다.
     *
     * @param request 로그인 요청 (이메일, 비밀번호)
     * @return JWT 토큰 쌍 (액세스 + 리프레시)
     * @throws BusinessException 이메일/비밀번호 불일치 시
     */
    public TokenResponse login(LoginRequest request) {
        // 1. 이메일로 사용자 조회
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("로그인 실패 - 존재하지 않는 이메일: {}", request.email());
                    return new BusinessException(ErrorCode.LOGIN_FAILED);
                });

        // 2. 비밀번호 검증 (BCrypt 해시 비교)
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("로그인 실패 - 비밀번호 불일치: userId={}", user.getUserId());
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        log.info("로그인 성공 - userId: {}, email: {}", user.getUserId(), user.getEmail());

        // 3. JWT 토큰 발급
        return generateTokens(user.getUserId());
    }

    /**
     * 리프레시 토큰으로 새 액세스 토큰을 발급합니다.
     *
     * @param refreshToken 리프레시 토큰 문자열
     * @return 새로운 JWT 토큰 쌍
     * @throws BusinessException 토큰이 유효하지 않거나 리프레시 토큰이 아닌 경우
     */
    public TokenResponse refreshToken(String refreshToken) {
        // 1. 토큰 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            log.warn("토큰 갱신 실패 - 유효하지 않은 리프레시 토큰");
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        // 2. 리프레시 토큰 타입 확인
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            log.warn("토큰 갱신 실패 - 리프레시 토큰이 아닌 토큰으로 갱신 시도");
            throw new BusinessException(ErrorCode.INVALID_TOKEN,
                    "리프레시 토큰이 아닌 토큰으로 갱신을 시도했습니다.");
        }

        // 3. 토큰에서 사용자 ID 추출
        String userId = jwtTokenProvider.extractUserId(refreshToken);

        // 4. 사용자 존재 여부 확인
        if (!userRepository.existsById(userId)) {
            log.warn("토큰 갱신 실패 - 존재하지 않는 사용자: userId={}", userId);
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        log.info("토큰 갱신 성공 - userId: {}", userId);

        // 5. 새 토큰 쌍 발급
        return generateTokens(userId);
    }

    /**
     * 현재 로그인한 사용자의 정보를 조회합니다.
     *
     * @param userId 사용자 ID (JWT에서 추출)
     * @return 사용자 정보 응답 DTO
     * @throws BusinessException 사용자를 찾을 수 없는 경우
     */
    public UserResponse getCurrentUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return UserResponse.from(user);
    }

    /**
     * 사용자 ID로 액세스 토큰과 리프레시 토큰을 생성합니다.
     *
     * @param userId 사용자 ID (String)
     * @return JWT 토큰 쌍
     */
    private TokenResponse generateTokens(String userId) {
        String accessToken = jwtTokenProvider.generateAccessToken(userId);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId);
        return new TokenResponse(accessToken, refreshToken);
    }
}
