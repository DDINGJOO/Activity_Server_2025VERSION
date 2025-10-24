# Activity-Server 향후 개발 및 개선 방향

> 작성일: 2025-10-24
> 프로젝트: Activity-Server (Teambind)
> 버전: 0.0.1-SNAPSHOT

---

## 목차

1. [프로젝트 현황 요약](#1-프로젝트-현황-요약)
2. [아키텍처 강점 분석](#2-아키텍처-강점-분석)
3. [개선이 필요한 영역](#3-개선이-필요한-영역)
4. [우선순위별 개발 로드맵](#4-우선순위별-개발-로드맵)
5. [세부 개선 방향](#5-세부-개선-방향)
6. [기술 부채 해결 방안](#6-기술-부채-해결-방안)
7. [장기 전략 및 확장성](#7-장기-전략-및-확장성)

---

## 1. 프로젝트 현황 요약

### 1.1 핵심 기능

- **사용자 활동 추적**: 게시글, 댓글, 좋아요 3가지 활동 기록 및 통계 관리
- **피드 제공**: 카테고리별 활동 피드 (커서 기반 페이징)
- **비동기 이벤트 처리**: Kafka를 통한 이벤트 기반 데이터 동기화
- **분산 스케줄링**: ShedLock 기반 아티클 동기화

### 1.2 기술 스택

- **백엔드**: Spring Boot 3.5.6 + Java 21
- **데이터베이스**: MariaDB (JPA/Hibernate)
- **캐시/큐**: Redis
- **메시징**: Apache Kafka
- **분산 락**: ShedLock (Redis 기반)

### 1.3 프로젝트 규모

- Java 파일: 34개
- 주요 엔티티: 4개 (UserArticle, UserComment, UserLike, UserBoardActivitiesCount)
- REST API 엔드포인트: 2개
- Kafka 토픽: 7개

---

## 2. 아키텍처 강점 분석

### 2.1 설계 우수 사례

#### ✅ 이벤트 기반 아키텍처

- **느슨한 결합**: Kafka를 통한 비동기 메시징으로 서비스 간 독립성 확보
- **확장성**: 신규 이벤트 추가 시 기존 코드 수정 최소화
- **내결함성**: 이벤트 재시도 로직 (Kafka retries: 3회)

#### ✅ 성능 최적화

- **집계 테이블**: `user_board_activities_count`로 통계 조회 성능 향상
	- 폴백 메커니즘: 집계 테이블 없을 시 실시간 COUNT 쿼리
- **커서 기반 페이징**: 대용량 데이터에서도 일정한 성능 보장
	- 오프셋 페이징 대비 O(N) → O(1) 복잡도
- **복합 인덱스**: `(article_synced, created_at)` - 동기화 상태 조회 최적화

#### ✅ 분산 시스템 대응

- **ShedLock**: 다중 인스턴스 환경에서 스케줄러 중복 실행 방지
- **Redis 기반 큐**: 미동기화 아티클 ID 관리로 스케줄러 효율화

#### ✅ 코드 품질

- **도메인 주도 설계 (DDD)**: 도메인별 패키지 분리 (board, article)
- **계층화 아키텍처**: Controller → Service → Repository 명확한 역할 분리
- **Lombok 활용**: 보일러플레이트 코드 감소

---

## 3. 개선이 필요한 영역

### 3.1 긴급 (Critical)

#### ❗ 데이터 일관성 취약점

**문제점:**

```java
// ArticleEventConsumer.java:32-37
if(article.isEmpty()){
		userArticleRepository.

save(new UserArticle(...));
		userBoardActivitiesCountRepository.

findById(request.getWriterId())
		.

ifPresent(UserBoardActivitiesCount::increaseArticleCount);
}
```

- **@Transactional 누락**: 게시글 저장과 통계 증가가 별도 트랜잭션
- **Race Condition**: 동시 요청 시 통계 불일치 가능
- **위치**: `ArticleEventConsumer`, `CommentEventConsumer`, `LikeEventConsumer`

**해결 방안:**

```java

@Transactional
public void increaseArticleRequest(String message) {
	// 전체 로직을 단일 트랜잭션으로 묶기
}
```

#### ❗ 에러 처리 부재

**문제점:**

- Kafka Consumer에 예외 처리 없음 → 메시지 유실 위험
- ArticleClient 실패 시 빈 리스트 반환 → 동기화 실패 추적 불가
- 스케줄러 예외 로그만 출력 → 복구 메커니즘 부재

**해결 방안:**

- Dead Letter Queue (DLQ) 도입
- 재시도 정책 (Exponential Backoff)
- 에러 모니터링 및 알림

#### ❗ 테스트 부재

- 단위 테스트 없음 (현재 빈 테스트 파일만 존재)
- 통합 테스트 없음
- 이벤트 기반 시스템의 복잡도 대비 테스트 커버리지 0%

---

### 3.2 중요 (High Priority)

#### 🔴 보안 취약점

**인증/인가 검증 강화 필요:**

```java
// FeedController.java:69-71
if("like".equalsIgnoreCase(category) &&
		(viewerId ==null||!viewerId.

equals(targetUserId))){
		return ResponseEntity.

status(403).

build();
}
```

- viewerId는 클라이언트 요청 파라미터 → 위조 가능
- **별도 인증 서버 연동**: API Gateway 또는 Auth Server에서 검증된 사용자 ID를 헤더로 전달받는 구조 필요
- 현재는 클라이언트가 임의의 viewerId를 전송할 수 있는 구조

**환경 변수 보안:**

- `.env` 파일 Git 히스토리에 노출 위험
- 민감 정보 암호화 부재 (DB 패스워드, Kafka URL 등)

#### 🔴 모니터링 및 관측성 부족

- 메트릭 수집 시스템 없음 (Micrometer, Prometheus 등)
- 분산 추적 부재 (Sleuth, Zipkin 등)
- 로깅: 기본 Slf4j만 사용 (구조화된 로그 부재)

#### 🔴 동기화 로직 개선 필요

**ArticleSyncScheduler 비효율성:**

```java
// 60분마다만 DB 조회
if(emptyCheckCounter >=60){
dbArticleIds =

findUnsyncedArticleIdsFromDB();
}
```

- **문제**: Redis 장애 시 최대 60분 동기화 지연
- **개선**: Redis 상태 체크 → 장애 시 즉시 DB 폴백

**배치 크기 하드코딩:**

```java
// 100개씩 배치 조회
for(int i = 0; i <articleIds.

size();

i +=100){
```

- 설정 파일로 외부화 필요

---

### 3.3 개선 권장 (Medium Priority)

#### 🟡 API 설계 개선

**RESTful 규칙 미준수:**

```
POST /api/board/feed  (통계 조회)  ❌
→ GET /api/board/users/{userId}/activities/stats  ✅
```

**응답 구조 불일치:**

```java
// FeedResponse.java - 두 가지 용도로 사용
new FeedResponse(totals, isOwner);     // POST 요청
new

FeedResponse(articleIds, cursor);  // GET 요청
```

- 단일 DTO를 다른 용도로 재사용 → 명확성 저하

#### 🟡 데이터베이스 최적화

**인덱스 추가 필요:**

```sql
-- user_article 테이블
CREATE INDEX idx_user_created ON user_article (user_id, created_at DESC);

-- user_board_activities_count 테이블
CREATE INDEX idx_updated_at ON user_board_activities_count (updated_at);
```

**N+1 문제 잠재적 위험:**

```java
// ArticleSyncScheduler.java:165-180
List<UserComment> comments = userCommentRepository.findAllByIdArticleId(dto.getArticleId());
comments.

forEach(comment ->{...});

List<UserLike> likes = userLikeRepository.findAllByIdArticleId(dto.getArticleId());
likes.

forEach(like ->{...});
```

- Batch Update 고려 필요

#### 🟡 설정 관리 개선

- 환경변수 의존성 과다 (12개 필수 환경변수)
- Spring Cloud Config 도입 고려
- Feature Flag 시스템 부재

---

## 4. 우선순위별 개발 로드맵

### Phase 1: 안정성 확보 (1-2개월)

#### 목표: 프로덕션 환경 안정성 확보

| 작업                               | 우선순위 | 예상 공수 | 담당 영역         |
|----------------------------------|------|-------|---------------|
| Kafka Consumer @Transactional 적용 | P0   | 3일    | Messaging     |
| Dead Letter Queue (DLQ) 구현       | P0   | 5일    | Messaging     |
| 단위 테스트 작성 (커버리지 70% 목표)          | P0   | 10일   | 전체            |
| 통합 테스트 (Testcontainers)          | P1   | 5일    | Integration   |
| 에러 모니터링 (Sentry/ELK) 도입          | P1   | 3일    | Observability |
| API 인증/인가 구현 (JWT)               | P0   | 7일    | Security      |

**산출물:**

- 테스트 커버리지 리포트
- DLQ 처리 프로세스 문서
- 인증 인가 가이드

---

### Phase 2: 성능 및 확장성 개선 (2-3개월)

#### 목표: 대규모 트래픽 대응

| 작업                               | 우선순위 | 예상 공수 | 담당 영역          |
|----------------------------------|------|-------|----------------|
| 메트릭 수집 (Micrometer + Prometheus) | P1   | 5일    | Observability  |
| 분산 추적 (Sleuth + Zipkin)          | P1   | 5일    | Observability  |
| 데이터베이스 인덱스 최적화                   | P1   | 3일    | Database       |
| Redis 클러스터 구성                    | P2   | 7일    | Infrastructure |
| 배치 업데이트 최적화                      | P1   | 5일    | Performance    |
| API Rate Limiting                | P2   | 3일    | API            |

**산출물:**

- 성능 테스트 리포트 (k6/JMeter)
- Grafana 대시보드
- 용량 계획 문서

---

### Phase 3: 기능 확장 (3-6개월)

#### 목표: 사용자 가치 증대

| 작업                        | 우선순위 | 예상 공수 | 담당 영역       |
|---------------------------|------|-------|-------------|
| 활동 통계 시간 범위 조회 (일/주/월)    | P2   | 7일    | Feature     |
| 활동 필터링 (카테고리, 날짜)         | P2   | 5일    | Feature     |
| 실시간 활동 브로드캐스트 (WebSocket) | P3   | 10일   | Feature     |
| 활동 내보내기 (CSV/Excel)       | P3   | 5일    | Feature     |
| 알림 시스템 연동                 | P2   | 7일    | Integration |
| GraphQL API 제공            | P3   | 10일   | API         |

**산출물:**

- API 문서 (OpenAPI 3.0)
- 사용자 가이드
- 성능 벤치마크

---

### Phase 4: 운영 효율화 (지속적)

#### 목표: 운영 비용 절감 및 안정성 향상

| 작업              | 우선순위 | 예상 공수 | 담당 영역          |
|-----------------|------|-------|----------------|
| CI/CD 파이프라인 고도화 | P1   | 5일    | DevOps         |
| Blue-Green 배포   | P2   | 7일    | DevOps         |
| 자동 스케일링 (HPA)   | P1   | 5일    | Infrastructure |
| 백업 및 복구 자동화     | P1   | 5일    | Database       |
| 비용 모니터링 및 최적화   | P2   | 3일    | FinOps         |

---

## 5. 세부 개선 방향

### 5.1 데이터 일관성 강화

#### 현재 문제점

```java
// CommentEventConsumer.java:32-48
userCommentRepository.findById(key).

ifPresentOrElse(
		existing ->{},  // 중복 무시
		()->{
		userCommentRepository.

save(new UserComment(key));  // ①
		
		if(!userArticleRepository.

existsById(articleKey)){  // ②
		articleSyncQueue.

addMissingArticle(request.getArticleId());
		}
		
		userBoardActivitiesCountRepository.

findById(request.getWriterId())
		.

ifPresent(UserBoardActivitiesCount::increaseCommentCount);  // ③
    }
		    );
```

- ①②③ 각 작업이 별도 커밋 → 부분 실패 시 데이터 불일치

#### 개선 방안

**1. 트랜잭션 경계 명확화**

```java

@Transactional
public void increaseCommentRequest(String message) throws JsonProcessingException {
	// 전체 로직을 단일 트랜잭션으로 묶기
	// 실패 시 전체 롤백
}
```

**2. 낙관적 락 도입**

```java

@Entity
public class UserBoardActivitiesCount {
	@Version
	private Long version;
	
	// Hibernate가 자동으로 동시성 제어
}
```

**3. 이벤트 멱등성 보장**

```java
public class CommentCreatedEvent {
	private String eventId;  // UUID
	private Long timestamp;
}

// 중복 이벤트 필터링
if(eventRepository.

existsByEventId(event.getEventId())){
		log.

warn("중복 이벤트 무시: {}",event.getEventId());
		return;
		}
```

---

### 5.2 에러 처리 및 복원력

#### Dead Letter Queue 구현

**Kafka DLQ 설정:**

```yaml
spring:
  kafka:
    listener:
      error-handler:
        type: dead-letter-queue
        dead-letter-topic: activity-dlq
        max-retries: 3
        backoff:
          initial-interval: 1000
          multiplier: 2.0
          max-interval: 10000
```

**DLQ 처리 Consumer:**

```java

@KafkaListener(topics = "activity-dlq", groupId = "dlq-handler-group")
public void handleDeadLetterQueue(String message) {
	// 1. 에러 분석 및 로깅
	// 2. 관리자 알림
	// 3. 수동 재처리 대기열에 추가
}
```

#### Circuit Breaker (Article Client)

```java

@CircuitBreaker(name = "articleClient", fallbackMethod = "getArticlesByIdsFallback")
public List<ArticleDto> getArticlesByIds(List<String> articleIds) {
	// WebClient 호출
}

private List<ArticleDto> getArticlesByIdsFallback(List<String> articleIds, Exception e) {
	log.error("Article 서버 호출 실패, 재시도 큐에 추가", e);
	articleIds.forEach(articleSyncQueue::addMissingArticle);
	return Collections.emptyList();
}
```

---

### 5.3 테스트 전략

#### 단위 테스트 (JUnit 5 + Mockito)

**Repository 테스트:**

```java

@DataJpaTest
class UserArticleRepositoryTest {
	@Autowired
	private UserArticleRepository repository;
	
	@Test
	void 커서_페이징_최신순_조회() {
		// Given: 테스트 데이터 삽입
		// When: fetchDescByUserIdWithCursor 호출
		// Then: 정렬 순서 및 커서 검증
	}
}
```

**Service 테스트:**

```java

@ExtendWith(MockitoExtension.class)
class FeedDomainServiceTest {
	@Mock
	private UserArticleRepository articleRepository;
	
	@InjectMocks
	private FeedDomainService service;
	
	@Test
	void 집계_테이블_우선_조회() {
		// Given: Mock 데이터 설정
		// When: computeTotals 호출
		// Then: 집계 테이블 우선 사용 검증
	}
}
```

#### 통합 테스트 (Testcontainers)

```java

@SpringBootTest
@Testcontainers
class ArticleSyncIntegrationTest {
	@Container
	static MariaDBContainer mariaDB = new MariaDBContainer("mariadb:11");
	
	@Container
	static KafkaContainer kafka = new KafkaContainer(
			DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
	);
	
	@Test
	void 게시글_생성_이벤트_처리_통합_테스트() {
		// Given: Kafka에 article-created 이벤트 발행
		// When: Consumer가 이벤트 처리
		// Then: DB에 데이터 저장 및 통계 업데이트 검증
	}
}
```

---

### 5.4 보안 강화

#### 인증 서버 연동 구현

**아키텍처:**

```
[Client] → [API Gateway] → [Auth Server] → [Activity-Server]
                  ↓
          X-User-Id: {userId} 헤더 추가
```

**헤더 검증 필터:**

```java

@Component
public class AuthHeaderFilter extends OncePerRequestFilter {
	
	@Override
	protected void doFilterInternal(HttpServletRequest request,
	                                HttpServletResponse response,
	                                FilterChain filterChain) throws IOException, ServletException {
		
		// API Gateway 또는 Auth Server에서 전달한 사용자 ID
		String userId = request.getHeader("X-User-Id");
		
		if (userId != null) {
			// SecurityContext에 사용자 정보 설정
			UserPrincipal principal = new UserPrincipal(userId);
			Authentication auth = new PreAuthenticatedAuthenticationToken(
					principal, null, Collections.emptyList()
			);
			SecurityContextHolder.getContext().setAuthentication(auth);
		}
		
		filterChain.doFilter(request, response);
	}
}
```

**Controller 수정:**

```java

@GetMapping("/feed/{category}")
public ResponseEntity<FeedResponse> getCategoryFeed(
		@PathVariable String category,
		@RequestHeader(value = "X-User-Id", required = false) String viewerId,
		@RequestParam String targetUserId,
        ...
) {
	// viewerId는 API Gateway에서 검증된 값
	// 권한 검증 로직
	if ("like".equalsIgnoreCase(category) && !targetUserId.equals(viewerId)) {
		return ResponseEntity.status(403).build();
	}
	// ...
}
```

**API Gateway 레벨 검증 (Kong/Spring Cloud Gateway 예시):**

```yaml
# API Gateway에서 JWT 검증 후 헤더 추가
spring:
  cloud:
    gateway:
      routes:
        - id: activity-service
          uri: http://activity-server:8080
          predicates:
            - Path=/api/board/**
          filters:
            - name: JwtAuthenticationFilter  # JWT 검증
            - name: AddRequestHeader          # 검증된 userId 헤더 추가
              args:
                name: X-User-Id
                value: "#{claims['sub']}"
```

---

### 5.5 모니터링 및 관측성

#### Micrometer + Prometheus

**메트릭 수집:**

```java

@Component
public class ActivityMetrics {
	private final Counter articleCreatedCounter;
	private final Timer syncTimer;
	
	public ActivityMetrics(MeterRegistry registry) {
		this.articleCreatedCounter = Counter.builder("activity.article.created")
				.description("게시글 생성 이벤트 수")
				.tag("type", "article")
				.register(registry);
		
		this.syncTimer = Timer.builder("activity.sync.duration")
				.description("아티클 동기화 소요 시간")
				.register(registry);
	}
	
	public void recordArticleCreated() {
		articleCreatedCounter.increment();
	}
	
	public void recordSyncDuration(Runnable task) {
		syncTimer.record(task);
	}
}
```

**Grafana 대시보드 구성:**

- 초당 이벤트 처리량 (TPS)
- 동기화 성공/실패율
- API 응답 시간 (p50, p95, p99)
- Redis/Kafka 연결 상태

#### 분산 추적 (Spring Cloud Sleuth)

```yaml
spring:
  sleuth:
    sampler:
      probability: 0.1  # 10% 샘플링
  zipkin:
    base-url: http://zipkin:9411
```

**추적 컨텍스트 전파:**

```
[Request ID: abc123] POST /api/board/feed
  └─ [Span: service] FeedDomainService.computeTotals
      └─ [Span: db] UserBoardActivitiesCountRepository.findById
      └─ [Span: db] UserArticleRepository.countByIdUserId
```

---

### 5.6 API 개선

#### RESTful 재설계

**현재 API:**

```
POST /api/board/feed                          (통계 조회)
GET  /api/board/feed/{category}               (피드 조회)
```

**개선된 API:**

```
GET  /api/v1/users/{userId}/activities/stats                (통계 조회)
GET  /api/v1/users/{userId}/activities/articles             (게시글 피드)
GET  /api/v1/users/{userId}/activities/comments             (댓글 피드)
GET  /api/v1/users/{userId}/activities/likes                (좋아요 피드)
```

**버전 관리:**

- API 버전 명시 (/api/v1)
- 하위 호환성 유지 (v1 유지하며 v2 추가)

#### OpenAPI 3.0 문서화

```yaml
openapi: 3.0.0
info:
  title: Activity Server API
  version: 1.0.0
paths:
  /api/v1/users/{userId}/activities/stats:
    get:
      summary: 사용자 활동 통계 조회
      parameters:
        - name: userId
          in: path
          required: true
          schema:
            type: string
      responses:
        '200':
          description: 성공
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ActivityStats'
```

---

### 5.7 데이터베이스 최적화

#### 인덱스 전략

**추가 인덱스:**

```sql
-- 1. 사용자별 최신 활동 조회 (피드)
CREATE INDEX idx_user_article_created
    ON user_article (user_id, created_at DESC, article_id DESC);

CREATE INDEX idx_user_comment_created
    ON user_comment (user_id, created_at DESC, article_id DESC);

CREATE INDEX idx_user_like_created
    ON user_like (user_id, created_at DESC, article_id DESC);

-- 2. 아티클별 활동 조회 (통계)
CREATE INDEX idx_article_comments
    ON user_comment (article_id, created_at DESC);

CREATE INDEX idx_article_likes
    ON user_like (article_id, created_at DESC);

-- 3. 통계 테이블 갱신 추적
ALTER TABLE user_board_activities_count
    ADD COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

CREATE INDEX idx_activities_updated
    ON user_board_activities_count (updated_at);
```

#### 쿼리 최적화

**Batch Update:**

```java
// 개선 전: N번의 UPDATE
comments.forEach(comment ->{
		comment.

setTitle(dto.getTitle());
		// JPA가 각각 UPDATE 실행
		});

// 개선 후: 1번의 BATCH UPDATE
@Modifying
@Query("UPDATE UserComment c SET c.title = :title, c.version = :version, " +
		"c.createdAt = :createdAt, c.articleSynced = true " +
		"WHERE c.id.articleId = :articleId")
int batchUpdateByArticleId(@Param("articleId") String articleId,
                           @Param("title") String title,
                           @Param("version") Integer version,
                           @Param("createdAt") LocalDateTime createdAt);
```

**Read-Only 트랜잭션:**

```java

@Transactional(readOnly = true)
public Map<String, Long> computeTotals(String targetUserId, List<String> categories) {
	// 읽기 전용 최적화 (Dirty Checking 비활성화)
}
```

---

### 5.8 설정 관리 개선

#### Spring Cloud Config 도입

**Config Server 구성:**

```yaml
# application.yml (Config Server)
spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/teambind/activity-config
          default-label: main
```

**Activity Server 연동:**

```yaml
# bootstrap.yml
spring:
  application:
    name: activity-server
  cloud:
    config:
      uri: http://config-server:8888
      fail-fast: true
```

**환경별 설정 분리:**

```
activity-config/
├── activity-server.yml           (공통)
├── activity-server-dev.yml       (개발)
├── activity-server-prod.yml      (운영)
└── activity-server-local.yml     (로컬)
```

#### Feature Flag

```java

@Component
public class FeatureFlags {
	@Value("${feature.realtime-broadcast.enabled:false}")
	private boolean realtimeBroadcastEnabled;
	
	@Value("${feature.graphql-api.enabled:false}")
	private boolean graphqlApiEnabled;
	
	public boolean isRealtimeBroadcastEnabled() {
		return realtimeBroadcastEnabled;
	}
}
```

---

## 6. 기술 부채 해결 방안

### 6.1 코드 품질 개선

#### SonarQube 도입

**목표 지표:**

- Code Coverage: 80% 이상
- Code Smells: 0개
- Bugs: 0개
- Security Hotspots: 0개
- Technical Debt Ratio: < 5%

**CI/CD 통합:**

```yaml
# .github/workflows/ci.yml
- name: SonarQube Scan
  run: ./gradlew sonarqube \
    -Dsonar.projectKey=activity-server \
    -Dsonar.host.url=${{ secrets.SONAR_HOST_URL }} \
    -Dsonar.login=${{ secrets.SONAR_TOKEN }}
```

#### Code Review 체크리스트

**필수 검토 항목:**

- [ ] @Transactional 적절히 사용되었는가?
- [ ] 예외 처리가 누락되지 않았는가?
- [ ] 로깅이 적절한 레벨로 설정되었는가?
- [ ] 테스트 코드가 함께 작성되었는가?
- [ ] API 문서가 업데이트되었는가?

---

### 6.2 리팩토링 우선순위

#### 1단계: 중복 코드 제거

**문제 코드:**

```java
// ArticleEventConsumer, CommentEventConsumer, LikeEventConsumer 모두 유사한 패턴
userBoardActivitiesCountRepository.findById(userId)
    .

ifPresent(UserBoardActivitiesCount::increaseXxxCount);
```

**개선안: 공통 서비스 추출**

```java

@Service
public class ActivityCountService {
	
	@Transactional
	public void incrementCount(String userId, ActivityType type) {
		userBoardActivitiesCountRepository.findById(userId)
				.ifPresent(count -> count.increment(type));
	}
}
```

#### 2단계: 매직 넘버/문자열 제거

**개선 전:**

```java
if(emptyCheckCounter >=60){  // 60은 무엇?
		// ...
		}
		
		for(
int i = 0; i <articleIds.

size();

i +=100){  // 100은 무엇?
```

**개선 후:**

```java
// application.yml
activity:
sync:
empty-check-threshold:60  # 60분
batch-size:100

@ConfigurationProperties(prefix = "activity.sync")
public class SyncProperties {
	private final int emptyCheckThreshold = 60;
	private final int batchSize = 100;
}
```

---

## 7. 장기 전략 및 확장성

### 7.1 마이크로서비스 진화

#### 서비스 분리 전략

**현재 구조:**

```
Activity-Server (단일 서비스)
├── Board Activities (게시판 활동)
├── Article Sync (아티클 동기화)
└── Messaging (이벤트 처리)
```

**향후 구조 (MSA 진화):**

```
┌─────────────────────────┐
│  Activity-API-Gateway   │  (Kong, Spring Cloud Gateway)
└───────────┬─────────────┘
            │
    ┌───────┴────────┬────────────┬─────────────┐
    │                │            │             │
┌───▼────┐    ┌─────▼─────┐  ┌──▼──────┐  ┌──▼──────┐
│ Feed   │    │ Activity  │  │ Stats   │  │ Sync    │
│ Service│    │ Recorder  │  │ Service │  │ Worker  │
└────────┘    └───────────┘  └─────────┘  └─────────┘
```

**분리 기준:**

- Feed Service: 읽기 전용 (조회 최적화)
- Activity Recorder: 쓰기 전용 (이벤트 처리)
- Stats Service: 통계 집계 (CQRS 패턴)
- Sync Worker: 백그라운드 작업

---

### 7.2 CQRS 패턴 도입

#### 명령-조회 책임 분리

**Write Model (Command):**

```java

@Service
public class ActivityCommandService {
	public void recordArticleCreated(ArticleCreatedEvent event) {
		// 쓰기 최적화 DB (MariaDB)
		// 이벤트 소싱
	}
}
```

**Read Model (Query):**

```java

@Service
public class ActivityQueryService {
	public FeedResponse getUserFeed(String userId, String category) {
		// 읽기 최적화 DB (Elasticsearch, Cassandra)
		// 비정규화된 데이터
	}
}
```

**이벤트 소싱:**

```java

@Entity
public class ActivityEvent {
	@Id
	private String eventId;
	private String aggregateId;  // userId
	private String eventType;    // ARTICLE_CREATED
	private String payload;      // JSON
	private LocalDateTime occurredAt;
}
```

---

### 7.3 다중 지역 배포

#### 글로벌 확장 전략

**지역별 클러스터:**

```
┌────────────┐     ┌────────────┐     ┌────────────┐
│  US-EAST   │     │  EU-WEST   │     │  AP-SEOUL  │
│  (Primary) │────▶│  (Replica) │────▶│  (Replica) │
└────────────┘     └────────────┘     └────────────┘
      │                   │                   │
      └───────────────────┴───────────────────┘
                    Global Kafka
```

**데이터 동기화:**

- Active-Active 구성 (양방향 복제)
- Conflict Resolution (Last-Write-Wins)
- CDC (Change Data Capture) 활용

---

### 7.4 비용 최적화

#### 컴퓨팅 자원

**Auto Scaling 정책:**

```yaml
# HPA (Horizontal Pod Autoscaler)
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: activity-server
spec:
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
```

**Spot Instance 활용:**

- Sync Worker: 비상시성 작업 → Spot Instance 적합
- API Server: 가용성 중요 → On-Demand Instance

#### 데이터 스토리지

**냉/온 데이터 분리:**

```
Hot Data (최근 3개월)
  └─ Redis (캐시) + MariaDB (Primary)

Warm Data (3-12개월)
  └─ MariaDB (Replica, Read-Only)

Cold Data (12개월 이상)
  └─ S3 + Glacier (아카이빙)
```

**데이터 라이프사이클:**

```sql
-- 12개월 이상 데이터 아카이빙 (매월 1일 실행)
INSERT INTO s3_archive
SELECT *
FROM user_article
WHERE created_at < DATE_SUB(NOW(), INTERVAL 12 MONTH);

DELETE
FROM user_article
WHERE created_at < DATE_SUB(NOW(), INTERVAL 12 MONTH);
```

---

## 8. 실행 계획 및 마일스톤

### 8.1 3개월 로드맵 (Q1 2025)

#### Week 1-2: 긴급 개선

- [ ] Kafka Consumer @Transactional 적용
- [ ] 단위 테스트 프레임워크 구축
- [ ] 에러 모니터링 시스템 구축

#### Week 3-4: 보안 강화

- [ ] API Gateway/Auth Server 헤더 검증 연동
- [ ] API Rate Limiting
- [ ] 환경 변수 암호화

#### Week 5-6: 테스트 커버리지

- [ ] 단위 테스트 작성 (50% 커버리지)
- [ ] 통합 테스트 작성
- [ ] CI/CD에 테스트 통합

#### Week 7-8: 성능 최적화

- [ ] 데이터베이스 인덱스 최적화
- [ ] 배치 업데이트 구현
- [ ] Redis 클러스터 구성

#### Week 9-10: 관측성

- [ ] Prometheus + Grafana 대시보드
- [ ] 분산 추적 (Sleuth)
- [ ] 로그 구조화 (ELK)

#### Week 11-12: 안정화

- [ ] 부하 테스트 수행
- [ ] 장애 시나리오 테스트
- [ ] 운영 문서 작성

---

### 8.2 성공 지표 (KPI)

| 지표              | 현재 | 목표 (3개월) | 목표 (6개월) |
|-----------------|----|----------|----------|
| 테스트 커버리지        | 0% | 70%      | 85%      |
| API 응답 시간 (p99) | ?  | < 500ms  | < 300ms  |
| 이벤트 처리 실패율      | ?  | < 0.1%   | < 0.01%  |
| 시스템 가용성         | ?  | 99.9%    | 99.95%   |
| 배포 빈도           | ?  | 주 1회     | 일 1회     |
| 평균 복구 시간 (MTTR) | ?  | < 30분    | < 15분    |

---

### 8.3 리스크 관리

| 리스크      | 영향도 | 가능성 | 완화 전략                        |
|----------|-----|-----|------------------------------|
| Kafka 장애 | 높음  | 중간  | DLQ + 재시도 + Circuit Breaker  |
| Redis 장애 | 중간  | 낮음  | DB 폴백 + Redis Sentinel       |
| DB 성능 저하 | 높음  | 중간  | 인덱스 최적화 + Read Replica       |
| 대규모 트래픽  | 높음  | 높음  | Auto Scaling + Rate Limiting |
| 데이터 불일치  | 높음  | 낮음  | @Transactional + 정합성 체크 배치   |

---

## 9. 결론

### 9.1 현재 프로젝트 평가

**강점:**

- 이벤트 기반 아키텍처로 확장성 확보
- 성능 최적화 (집계 테이블, 커서 페이징)
- 분산 시스템 대응 (ShedLock)

**개선 필요:**

- 데이터 일관성 (@Transactional 누락)
- 테스트 부재 (0% 커버리지)
- 보안 취약점 (인증/인가 부재)
- 모니터링 부족 (메트릭, 추적 없음)

### 9.2 핵심 권장 사항

#### 즉시 실행 (1개월 이내)

1. **@Transactional 적용**: 데이터 일관성 확보
2. **단위 테스트 작성**: 최소 50% 커버리지
3. **JWT 인증 구현**: 보안 강화
4. **에러 모니터링**: Sentry/ELK 도입

#### 단기 실행 (3개월 이내)

1. **DLQ 구현**: 메시지 유실 방지
2. **성능 최적화**: 인덱스, 배치 업데이트
3. **관측성 구축**: Prometheus, Grafana, Sleuth
4. **CI/CD 고도화**: 자동화 배포

#### 중장기 실행 (6개월 이상)

1. **CQRS 패턴 도입**: 읽기/쓰기 분리
2. **서비스 분리**: MSA 진화
3. **글로벌 확장**: 다중 지역 배포
4. **비용 최적화**: Auto Scaling, 데이터 라이프사이클

### 9.3 기대 효과

**기술적 개선:**

- 시스템 안정성: 99.9% → 99.95%
- 응답 시간: 50% 단축
- 장애 복구: MTTR 70% 감소

**비즈니스 가치:**

- 사용자 경험 향상
- 운영 비용 절감 (30%)
- 확장성 확보 (10배 트래픽 대응)

---

## 부록

### A. 참고 자료

- [Spring Boot Best Practices](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Kafka Streams Documentation](https://kafka.apache.org/documentation/streams/)
- [Database Indexing Strategies](https://use-the-index-luke.com/)
- [CQRS Pattern](https://martinfowler.com/bliki/CQRS.html)

### B. 도구 및 라이브러리

- **테스트**: JUnit 5, Mockito, Testcontainers
- **모니터링**: Micrometer, Prometheus, Grafana
- **추적**: Spring Cloud Sleuth, Zipkin
- **보안**: Spring Security, JWT
- **문서화**: Swagger/OpenAPI, Spring REST Docs

### C. 연락처

- 프로젝트 관리자: [이메일]
- 기술 리드: [이메일]
- GitHub Issues: [Repository URL]

---

**문서 버전**: 1.0
**최종 수정**: 2025-10-24
**작성자**: Claude Code Analysis
**검토자**: [검토자명]
