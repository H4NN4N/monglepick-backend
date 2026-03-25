# monglepick-backend — Spring Boot 백엔드

> Spring Boot 4.0.4 + JPA + JWT + OAuth2 | 포트 8080 | Java 21 | Gradle

## 빠른 시작

```bash
cd /Users/yoonhyungjoo/Documents/monglepick/monglepick-backend
./gradlew compileJava          # 컴파일 확인
./gradlew bootRun              # 서버 실행 (localhost:8080)
./gradlew test                 # 테스트
```

## 프로젝트 구조 (DDD)

```
src/main/java/com/monglepick/monglepickbackend/
├── MonglepickBackendApplication.java
├── global/
│   ├── config/         # SecurityConfig, WebConfig, JpaConfig
│   ├── security/       # JWT 필터, ServiceKey 필터, JwtTokenProvider
│   ├── exception/      # BusinessException, ErrorCode, GlobalExceptionHandler
│   ├── entity/         # BaseTimeEntity, BaseAuditEntity
│   └── dto/            # ApiResponse
└── domain/             # 15개 도메인
    ├── auth/           # 회원가입/로그인/OAuth2/JWT Refresh Rotation
    ├── user/           # User, UserPreference
    ├── movie/          # Movie, Like, FavGenre, FavMovie
    ├── community/      # Post(DRAFT/PUBLISHED), PostComment, PostLike, Category
    ├── review/         # Review
    ├── watchhistory/   # WatchHistory, UserWishlist
    ├── reward/         # UserPoint, PointsHistory, PointItem, UserAttendance
    ├── payment/        # PaymentOrder(멱등키), UserSubscription, SubscriptionPlan
    ├── recommendation/ # RecommendationLog, RecommendationFeedback
    ├── chat/           # ChatSessionArchive
    ├── roadmap/        # RoadmapCourse, CourseReview, UserAchievement
    ├── content/        # MovieMention, ToxicityLog
    ├── search/         # SearchHistory, TrendingKeyword
    ├── playlist/       # Playlist, PlaylistItem
    └── admin/          # (구현 예정)
```

## 코딩 컨벤션

### Entity
- `BaseAuditEntity` 상속 (createdBy/createdAt/updatedBy/updatedAt)
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)` + `@Builder`
- PK: `{도메인}Id` 형식 (e.g., `postId`, `reviewId`)
- 관계 매핑: `@ManyToOne(fetch = LAZY)` 기본

### Service
- 클래스: `@Transactional(readOnly = true)`, 쓰기 메서드만 `@Transactional`
- DI: `@RequiredArgsConstructor` + `private final`
- 예외: `throw new BusinessException(ErrorCode.XXX)`

### Controller
- `@RestController` + `@RequestMapping("/api/v1/...")`
- 인증: `@AuthenticationPrincipal` 또는 `Principal`
- 응답: `ResponseEntity.ok()` / `ResponseEntity.status(CREATED).body()`

### DTO
- Java `record` (불변)
- `from(Entity)` 정적 팩토리 메서드
- 검증: `@NotBlank`, `@Min`, `@Email`, `@Pattern`

## 인증 구조

| 경로 | 인증 방식 | 설명 |
|------|----------|------|
| Client → Backend | JWT Bearer | userId는 토큰에서 추출 |
| Agent → Backend | X-Service-Key | userId는 Body에 포함 |
| OAuth2 | Spring Security OAuth2 Client | Google/Kakao/Naver |

## DDL 권위 원본

- **이 프로젝트가 DDL 원본** (`ddl-auto=update`, 37개 테이블)
- Recommend/Agent는 이 스키마에 맞춰 매핑

## 빌드 필수 확인

코드 수정 후 반드시 `./gradlew compileJava` 실행하여 빌드 성공 확인
