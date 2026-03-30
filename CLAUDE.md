# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

- **Build**: `./gradlew build`
- **Run (main app)**: `./gradlew :app:bootRun`
- **Run worker module (standalone JAR)**: `./gradlew :worker:bootRun`
- **Test all**: `./gradlew test`
- **Single test**: `./gradlew test --tests "com.walter.lifelog.SomeTest"`
- **Clean build**: `./gradlew clean build`

Requires Java 21+. Gradle 9.3.0 (wrapper included).

## Module Structure

9개 모듈의 Modular Monolith. 각 모듈은 저마다 고유한 `build.gradle.kts` 파일을 갖고 있습니다.

```
app/                     ← Spring Boot entry point, filter/security config
web/                     ← Thymeleaf MVC (Java), WebSocket, Kafka consumer
api/                     ← REST API controllers (Kotlin), AOP auth
shared/                  ← Common config, utilities (Java)
blog-service/            ← Post, Category, PostTag domain (Kotlin)
user-service/            ← User, Auth domain (Kotlin)
photo-archive-service/   ← Photo, GoogleDrive domain (Kotlin)
content-service/         ← Content domain (Kotlin)
worker/                  ← Kafka consumer, Scheduled jobs, Data sync (Kotlin)
```

**Language rule**: `shared`, `web` → Java.
All other modules → Kotlin.
`*-service` domain을 담당하는 모듈은 `shared` 모듈을 제외하고는 서로 직접 의존하지 않습니다. 

## Architecture Layers

```
Controller / ViewController
    ↓
Facade (@Facade)       ← 여러 Service 조합, 트랜잭션 경계
    ↓
Service                ← 단일 도메인 비즈니스 로직
    ↓
Repository / QueryRepository
    ↓
Entity
```

- **Controller**: REST는 `api/`, MVC View는 `web/`. 항상 `Rest<T>` 래퍼로 응답
- **Facade**: `@Facade` 커스텀 어노테이션 사용. 여러 Service나 Component를 조합할 때만 생성
- **Service**: 단일 도메인만 담당. 다른 Service 직접 의존 금지
- **QueryRepository**: 동적 쿼리는 jOOQ 사용 (code generation 없이 DSL.table/field string 방식)
- **Mapper**: Entity ↔ DTO 변환은 MapStruct(`@Mapper`) 사용

### 패키지 구조는 모듈마다 동일하게 유지

- `com.walter.lifelog.*.controller` — REST endpoints (e.g., `/post/{inquiryStr}` supports both ID and slug lookup)
- `com.walter.lifelog.*.facade` — Business logic
- `com.walter.lifelog.*.service` — Domain logic
- `com.walter.lifelog.*.repository` — Spring Data JPA repositories
- `com.walter.lifelog.*.entity` — JPA entities (Post, Category, User) with `entity/code/` for enums (PostStatus)
- `com.walter.lifelog.*.config.exception` — Custom exceptions (PostNotFoundException). `shared` 모듈의 `exception` 패키지에만 공통 예외 (e.g., `ResourceNotFoundException`)로 정의하여 위치

## Naming Conventions

### Kotlin / Java 공통
- 클래스: `*Controller`, `*Service`, `*Repository`, `*QueryRepository`, `*Facade`, `*Mapper`
- DTO: `*Request`, `*Response`, `*Condition`, `*Info` (접미사로 역할 명시)
- Config: `*Config`, `*Properties`
- Util: `*Helper`, `*Handler`, `*Supporter`

### JPA Entity
- ID 필드: `{entityName}Seq` (예: `postSeq`, `userSeq`, `categorySeq`)
- ID 생성 전략: `GenerationType.IDENTITY`
- 컬럼명: `snake_case`, 필드명: `camelCase`
- Enum 컬럼: `@Enumerated(EnumType.STRING)`
- 타임스탬프: `@CreationTimestamp` / `@UpdateTimestamp` (Hibernate)
- 논리 삭제: `is_active` Boolean 필드 (물리 삭제 사용 금지)
- Entity: Kotlin의 `data class`로 선언

### DB 테이블명
- 복수형 snake_case: `posts`, `categories`, `posts_tags`, `users`

### Kotlin 코딩 스타일
- `-Xjsr305=strict` 적용 — nullable 타입 명시 필수
- scope function 적극 활용 (`apply`, `let`, `run`, `also`)
- `data class`로 DTO/Entity 선언
- `companion object`에 상수 정의
- 함수 30줄 이하 유지

### jOOQ QueryRepository 패턴
```kotlin
companion object {
    private val TABLE = DSL.table("table_name")
    private val FIELD = DSL.field("table.column", Type::class.java)
}
```
비동기 병렬 쿼리는 `asyncSupply(virtualThreadExecutor) { ... }` 사용.

## Test Conventions

- 테스트 프레임워크: JUnit 5 + MockK + AssertJ
- `@DisplayName("한국어로 테스트 의도 설명")`
- `given / when / then` 주석 블록 유지
- Mock 생성: `mockk()` (Kotlin), `Mockito` (Java)
- 생성자 주입으로 테스트 대상 직접 생성 (Spring Context 불필요한 경우)
- `verify(exactly = 1) { ... }` 로 호출 횟수 검증
- 테스트용 fixture는 `private fun create*()` 헬퍼 메서드로 분리

## Key Technical Decisions

- **비동기**: `AsyncSupporter.asyncSupply(virtualThreadExecutor, supplier)` 사용
- **VirtualThread**: `VirtualThreadConfig`에서 Semaphore로 동시성 제한 (`availableProcessors * 2`)
- **캐시**: `@DynamicCacheable` 커스텀 어노테이션 (TTL 분 단위 설정)
- **인증 AOP**: `@AdminRequired` → `AdminRequiredAspect` (JWT 또는 Session)
- **설정 암호화**: Jasypt (`ENC(...)` 형식)
- **API 응답**: `Rest<T>` 래퍼 (`Rest.ok(data)`). 단, 인증을 처리하는 `api` 모듈의 `AuthController`는 `Rest` 래퍼 없이 응답 DTO를 직접 반환 (JWT 토큰 발급 등)
- **API Docs**: API의 Request/Response DTO는 Swagger/OpenAPI로 문서화. `@Schema` 어노테이션으로 필드 설명 추가.

## Do NOT

- `application.yml`에 `thymeleaf.cache: false` 추가 금지 — live 프로파일에 영향
- `VirtualThreadConfig` 수정 시 Semaphore 제거 금지 — 무제한 스레드 생성으로 CPU 급등 발생
- Entity 물리 삭제 금지 — 반드시 `is_active = false` soft delete 사용
- Service에서 다른 Service 직접 의존 금지 — Facade를 통해 조합
- DDL auto `create` / `create-drop` 을 live 프로파일에 사용 금지 — `validate`만 허용
- `shared` 모듈에서 특정 서비스 모듈 의존 금지 (순환 참조)

## Database Profiles

- **Default** (`application.yml`): H2 in-memory (MySQL 호환 모드), DDL `create-drop`
- **Dev** (`application-dev.yml`): 로컬 개발용
- **Live** (`application-live.yml`): MySQL, DDL `validate`, HikariCP (`max-pool: 10`)

## External Services (Live)

- **DB**: Aiven MySQL, Aiven PostgreSQL (SSL required)
- **Cache**: Aiven Valkey (Redis 호환, `rediss://` TLS)
- **Kafka**: Aiven Kafka (SSL 인증서 필요)
- **MongoDB**: MongoDB Atlas
- **Google Drive**: 사진 아카이브 저장소

## Observability

- **Metric**: Micrometer + Prometheus (`/actuator/prometheus`, metrics at actuator endpoints: health, info, prometheus)
- **Tracing**: OpenTelemetry Agent → Tempo (`http://127.0.0.1:4318/v1/traces`, live only)

## Commit Message

- 한국어로 작성 (예: `게시글 조회 버그 수정`, `OpenAI 자동 요약 기능 추가`)
