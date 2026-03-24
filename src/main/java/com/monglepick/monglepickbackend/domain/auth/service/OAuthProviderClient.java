package com.monglepick.monglepickbackend.domain.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monglepick.monglepickbackend.domain.user.entity.User;
import com.monglepick.monglepickbackend.global.exception.BusinessException;
import com.monglepick.monglepickbackend.global.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * OAuth 소셜 로그인 제공자 클라이언트.
 *
 * <p>Google, Kakao, Naver의 OAuth 2.0 토큰 교환 및 사용자 정보 조회를
 * 통합적으로 처리한다. 각 제공자별 API 응답 형식이 다르므로
 * 내부에서 정규화하여 {@link OAuthUserInfo}로 변환한다.</p>
 *
 * <h3>OAuth 흐름</h3>
 * <ol>
 *   <li>클라이언트가 소셜 제공자로부터 인가 코드(code)를 받음</li>
 *   <li>{@link #exchangeCode} — 인가 코드를 Access Token으로 교환</li>
 *   <li>{@link #fetchUserInfo} — Access Token으로 사용자 프로필 조회</li>
 * </ol>
 *
 * <h3>지원 제공자</h3>
 * <ul>
 *   <li><b>Google</b>: {@code /oauth2/v2/userinfo} → id, email, name, picture</li>
 *   <li><b>Kakao</b>: {@code /v2/user/me} → id, kakao_account.email, profile.nickname, profile.profile_image_url</li>
 *   <li><b>Naver</b>: {@code /v1/nid/me} → response.id, response.email, response.nickname, response.profile_image</li>
 * </ul>
 *
 * @see com.monglepick.monglepickbackend.domain.auth.service.AuthService
 */
@Slf4j
@Component
public class OAuthProviderClient {

    // ── Google OAuth 설정값 ──

    /** Google OAuth 클라이언트 ID */
    @Value("${app.oauth.google.client-id:}")
    private String googleClientId;

    /** Google OAuth 클라이언트 시크릿 */
    @Value("${app.oauth.google.client-secret:}")
    private String googleClientSecret;

    /** Google OAuth 토큰 교환 URI */
    @Value("${app.oauth.google.token-uri:https://oauth2.googleapis.com/token}")
    private String googleTokenUri;

    /** Google OAuth 사용자 정보 조회 URI */
    @Value("${app.oauth.google.userinfo-uri:https://www.googleapis.com/oauth2/v2/userinfo}")
    private String googleUserinfoUri;

    // ── Kakao OAuth 설정값 ──

    /** Kakao OAuth 클라이언트 ID (REST API 키) */
    @Value("${app.oauth.kakao.client-id:}")
    private String kakaoClientId;

    /** Kakao OAuth 클라이언트 시크릿 */
    @Value("${app.oauth.kakao.client-secret:}")
    private String kakaoClientSecret;

    /** Kakao OAuth 토큰 교환 URI */
    @Value("${app.oauth.kakao.token-uri:https://kauth.kakao.com/oauth/token}")
    private String kakaoTokenUri;

    /** Kakao OAuth 사용자 정보 조회 URI */
    @Value("${app.oauth.kakao.userinfo-uri:https://kapi.kakao.com/v2/user/me}")
    private String kakaoUserinfoUri;

    // ── Naver OAuth 설정값 ──

    /** Naver OAuth 클라이언트 ID */
    @Value("${app.oauth.naver.client-id:}")
    private String naverClientId;

    /** Naver OAuth 클라이언트 시크릿 */
    @Value("${app.oauth.naver.client-secret:}")
    private String naverClientSecret;

    /** Naver OAuth 토큰 교환 URI */
    @Value("${app.oauth.naver.token-uri:https://nid.naver.com/oauth2.0/token}")
    private String naverTokenUri;

    /** Naver OAuth 사용자 정보 조회 URI */
    @Value("${app.oauth.naver.userinfo-uri:https://openapi.naver.com/v1/nid/me}")
    private String naverUserinfoUri;

    /** JSON 파싱용 ObjectMapper (스레드 안전) */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** HTTP 클라이언트 (Spring 6+ RestClient) */
    private RestClient restClient;

    /**
     * 빈 초기화 시 RestClient를 생성한다.
     */
    @PostConstruct
    public void init() {
        this.restClient = RestClient.create();
        log.info("OAuth 프로바이더 클라이언트 초기화 완료 — Google: {}, Kakao: {}, Naver: {}",
                !googleClientId.isBlank(), !kakaoClientId.isBlank(), !naverClientId.isBlank());
    }

    /**
     * 인가 코드를 소셜 제공자의 Access Token으로 교환한다.
     *
     * <p>OAuth 2.0 Authorization Code Grant 흐름의 토큰 교환 단계이다.
     * 각 제공자의 토큰 엔드포인트에 POST 요청을 보내 Access Token을 받는다.</p>
     *
     * @param provider    소셜 로그인 제공자 (GOOGLE, KAKAO, NAVER)
     * @param code        인가 코드 (소셜 제공자로부터 받은 코드)
     * @param redirectUri 리다이렉트 URI (토큰 교환 시 인가 요청과 동일해야 함)
     * @return 소셜 제공자의 Access Token 문자열
     * @throws BusinessException 토큰 교환 실패 시 (ErrorCode.OAUTH_FAILED)
     */
    public String exchangeCode(User.Provider provider, String code, String redirectUri) {
        try {
            // 제공자별 설정값 조회
            String tokenUri = getTokenUri(provider);
            String clientId = getClientId(provider);
            String clientSecret = getClientSecret(provider);

            // 토큰 교환 요청 본문 구성 (MultiValueMap — URL 인코딩 자동 처리)
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("grant_type", "authorization_code");
            formData.add("code", code);
            formData.add("client_id", clientId);
            formData.add("client_secret", clientSecret);

            // redirectUri가 있으면 요청에 포함
            if (redirectUri != null && !redirectUri.isBlank()) {
                formData.add("redirect_uri", redirectUri);
            }

            log.debug("OAuth 토큰 교환 요청 — provider: {}, tokenUri: {}", provider, tokenUri);

            // POST 요청으로 토큰 교환
            String responseBody = restClient.post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(String.class);

            // 응답에서 access_token 추출
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            String accessToken = jsonNode.path("access_token").asText();

            if (accessToken == null || accessToken.isBlank()) {
                log.error("OAuth 토큰 교환 응답에 access_token이 없음 — provider: {}", provider);
                throw new BusinessException(ErrorCode.OAUTH_FAILED);
            }

            log.debug("OAuth 토큰 교환 성공 — provider: {}", provider);
            return accessToken;

        } catch (BusinessException e) {
            throw e;  // BusinessException은 그대로 전파
        } catch (Exception e) {
            log.error("OAuth 토큰 교환 실패 — provider: {}, error: {}", provider, e.getMessage(), e);
            throw new BusinessException(ErrorCode.OAUTH_FAILED);
        }
    }

    /**
     * 소셜 제공자의 Access Token으로 사용자 프로필을 조회한다.
     *
     * <p>각 제공자마다 응답 JSON 구조가 다르므로,
     * 내부에서 정규화하여 {@link OAuthUserInfo}로 변환한다.</p>
     *
     * @param provider    소셜 로그인 제공자 (GOOGLE, KAKAO, NAVER)
     * @param accessToken 소셜 제공자의 Access Token
     * @return 정규화된 사용자 프로필 정보
     * @throws BusinessException 사용자 정보 조회 실패 시 (ErrorCode.OAUTH_FAILED)
     */
    public OAuthUserInfo fetchUserInfo(User.Provider provider, String accessToken) {
        try {
            String userinfoUri = getUserinfoUri(provider);

            log.debug("OAuth 사용자 정보 조회 — provider: {}, userinfoUri: {}", provider, userinfoUri);

            // GET 요청 (Authorization: Bearer 헤더)
            String responseBody = restClient.get()
                    .uri(userinfoUri)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(String.class);

            JsonNode jsonNode = objectMapper.readTree(responseBody);

            // 제공자별 JSON 파싱
            OAuthUserInfo userInfo = switch (provider) {
                case GOOGLE -> parseGoogleUserInfo(jsonNode);
                case KAKAO -> parseKakaoUserInfo(jsonNode);
                case NAVER -> parseNaverUserInfo(jsonNode);
                default -> throw new BusinessException(ErrorCode.OAUTH_FAILED);
            };

            log.debug("OAuth 사용자 정보 조회 성공 — provider: {}, providerId: {}",
                    provider, userInfo.providerId());
            return userInfo;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("OAuth 사용자 정보 조회 실패 — provider: {}, error: {}", provider, e.getMessage(), e);
            throw new BusinessException(ErrorCode.OAUTH_FAILED);
        }
    }

    // ──────────────────────────────────────────────
    // 제공자별 JSON 파싱 (private 메서드)
    // ──────────────────────────────────────────────

    /**
     * Google 사용자 정보 JSON을 파싱한다.
     *
     * <p>Google 응답 형식: {@code { "id": "...", "email": "...", "name": "...", "picture": "..." }}</p>
     *
     * @param json Google API 응답 JSON
     * @return 정규화된 사용자 정보
     */
    private OAuthUserInfo parseGoogleUserInfo(JsonNode json) {
        return new OAuthUserInfo(
                json.path("id").asText(),
                json.path("email").asText(null),
                json.path("name").asText(null),
                json.path("picture").asText(null)
        );
    }

    /**
     * Kakao 사용자 정보 JSON을 파싱한다.
     *
     * <p>Kakao 응답 형식:
     * {@code { "id": 12345, "kakao_account": { "email": "...", "profile": { "nickname": "...", "profile_image_url": "..." } } }}</p>
     *
     * @param json Kakao API 응답 JSON
     * @return 정규화된 사용자 정보
     */
    private OAuthUserInfo parseKakaoUserInfo(JsonNode json) {
        JsonNode account = json.path("kakao_account");
        JsonNode profile = account.path("profile");

        return new OAuthUserInfo(
                String.valueOf(json.path("id").asLong()),
                account.path("email").asText(null),
                profile.path("nickname").asText(null),
                profile.path("profile_image_url").asText(null)
        );
    }

    /**
     * Naver 사용자 정보 JSON을 파싱한다.
     *
     * <p>Naver 응답 형식:
     * {@code { "response": { "id": "...", "email": "...", "nickname": "...", "profile_image": "..." } }}</p>
     *
     * @param json Naver API 응답 JSON
     * @return 정규화된 사용자 정보
     */
    private OAuthUserInfo parseNaverUserInfo(JsonNode json) {
        JsonNode resp = json.path("response");

        return new OAuthUserInfo(
                resp.path("id").asText(),
                resp.path("email").asText(null),
                resp.path("nickname").asText(null),
                resp.path("profile_image").asText(null)
        );
    }

    // ──────────────────────────────────────────────
    // 제공자별 설정값 조회 헬퍼 (private 메서드)
    // ──────────────────────────────────────────────

    /**
     * 제공자별 토큰 교환 URI를 반환한다.
     *
     * @param provider 소셜 로그인 제공자
     * @return 토큰 교환 URI 문자열
     */
    private String getTokenUri(User.Provider provider) {
        return switch (provider) {
            case GOOGLE -> googleTokenUri;
            case KAKAO -> kakaoTokenUri;
            case NAVER -> naverTokenUri;
            default -> throw new BusinessException(ErrorCode.OAUTH_FAILED);
        };
    }

    /**
     * 제공자별 사용자 정보 조회 URI를 반환한다.
     *
     * @param provider 소셜 로그인 제공자
     * @return 사용자 정보 조회 URI 문자열
     */
    private String getUserinfoUri(User.Provider provider) {
        return switch (provider) {
            case GOOGLE -> googleUserinfoUri;
            case KAKAO -> kakaoUserinfoUri;
            case NAVER -> naverUserinfoUri;
            default -> throw new BusinessException(ErrorCode.OAUTH_FAILED);
        };
    }

    /**
     * 제공자별 클라이언트 ID를 반환한다.
     *
     * @param provider 소셜 로그인 제공자
     * @return 클라이언트 ID 문자열
     */
    private String getClientId(User.Provider provider) {
        return switch (provider) {
            case GOOGLE -> googleClientId;
            case KAKAO -> kakaoClientId;
            case NAVER -> naverClientId;
            default -> throw new BusinessException(ErrorCode.OAUTH_FAILED);
        };
    }

    /**
     * 제공자별 클라이언트 시크릿을 반환한다.
     *
     * @param provider 소셜 로그인 제공자
     * @return 클라이언트 시크릿 문자열
     */
    private String getClientSecret(User.Provider provider) {
        return switch (provider) {
            case GOOGLE -> googleClientSecret;
            case KAKAO -> kakaoClientSecret;
            case NAVER -> naverClientSecret;
            default -> throw new BusinessException(ErrorCode.OAUTH_FAILED);
        };
    }

    // ──────────────────────────────────────────────
    // 내부 record
    // ──────────────────────────────────────────────

    /**
     * 소셜 로그인 제공자로부터 받은 정규화된 사용자 정보.
     *
     * <p>각 제공자(Google/Kakao/Naver)의 서로 다른 JSON 응답 형식을
     * 하나의 통일된 형태로 변환한 결과이다.</p>
     *
     * @param providerId   제공자가 발급한 사용자 고유 ID
     * @param email        이메일 주소 (nullable, 동의하지 않은 경우)
     * @param nickname     닉네임 (nullable)
     * @param profileImage 프로필 이미지 URL (nullable)
     */
    public record OAuthUserInfo(
            String providerId,
            String email,
            String nickname,
            String profileImage
    ) {
    }
}
