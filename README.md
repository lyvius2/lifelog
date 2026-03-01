# Lifelog 📝

> 개인 포트폴리오 프로젝트 — 일상을 기록하는 블로그/라이프로그 플랫폼

Spring Boot 4.0과 Kotlin/Java를 활용한 풀스택 웹 애플리케이션입니다.  
RESTful API 설계, JPA 기반 데이터 모델링, Spring Security 인증, 클라우드 네이티브 아키텍처를 적용했습니다.

## 주요 기능

- **게시글 관리**: CRUD 및 ID/Slug 기반 조회, Markdown 원본 보존
- **Markdown 에디터**: Commonmark 기반 Markdown → HTML 변환, 에디터 UI에서 카테고리·태그 입력 지원
- **관리자 인증**: Spring Security + 세션 기반 로그인/로그아웃, BCrypt 암호화
- **카테고리 시스템**: 계층 구조(Self-referencing) 카테고리
- **태그 시스템**: 게시글에 태그를 부여하는 다대다(M:N) 관계
- **콘텐츠 관리**: JSON 기반 유연한 콘텐츠 저장 (자기소개, 애차 소개 등)
- **사진 갤러리**: 이미지 업로드 및 갤러리 뷰
- **SSR 페이지**: Thymeleaf 템플릿 기반 서버 사이드 렌더링
- **레이아웃 데코레이터 패턴**: Thymeleaf Layout Dialect로 공통 nav/footer 분리
- **반응형 UI**: 네비게이션, 푸터 포함 모바일/데스크톱 대응
- **공통 API 응답 형식**: `Rest<T>` 제네릭 래퍼로 일관된 JSON 응답 구조
- **API 문서화**: Swagger UI 자동 생성

## 기술 스택

| 구분 | 기술 |
|------|------|
| **Language** | Kotlin 2.2.21, Java 21 |
| **Framework** | Spring Boot 4.0.2 |
| **Security** | Spring Security (세션 인증, BCrypt, CSRF 비활성) |
| **ORM** | Spring Data JPA, Hibernate |
| **Template Engine** | Thymeleaf (SSR) + Thymeleaf Layout Dialect (Decorator Pattern) |
| **Markdown** | Commonmark 0.24.0 (GFM Tables, Strikethrough, Autolink, Heading Anchor, Task List) |
| **Database** | H2 (개발), MySQL (운영) |
| **Build Tool** | Gradle 9.3.0 (Kotlin DSL) |
| **API Documentation** | Springdoc OpenAPI 2.7.0 (Swagger) |
| **Object Mapping** | MapStruct 1.6.3 |
| **Validation** | Spring Boot Starter Validation |
| **Monitoring** | Spring Boot Actuator, Micrometer, Prometheus |
| **Cloud** | Spring Cloud 2025.1.0 |
| | - OpenFeign (선언적 HTTP 클라이언트) |
| | - Load Balancer (클라이언트 사이드 로드밸런싱) |
| | - Resilience4j (서킷 브레이커) |

## 아키텍처

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Presentation Layer                           │
│  ┌────────────────┐ ┌──────────────────┐ ┌───────────────────────┐ │
│  │ PostController │ │ContentController │ │ RenderingController   │ │
│  │  (REST API)    │ │  (SSR + Model)   │ │   (SSR Pages)         │ │
│  └───────┬────────┘ └────────┬─────────┘ └─────────┬─────────────┘ │
│  ┌────────────────┐ ┌──────────────────┐                           │
│  │AuthController  │ │PostViewController│                           │
│  │ (REST 인증 API) │ │  (에디터 SSR)    │                           │
│  └───────┬────────┘ └────────┬─────────┘                           │
└──────────┼───────────────────┼───────────────────────────────────── ┘
           │                   │
┌──────────┼───────────────────┼───────────────────────────────────── ┐
│          ▼                   ▼           Security Layer              │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  SecurityConfig  ─►  CustomUserDetailsService  ─►  BCrypt   │   │
│  │  (세션 인증, CSRF off, 로그아웃 핸들링, 동시 세션 제한)        │   │
│  └──────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
           │
┌──────────┼───────────────────────────────────────────────────────── ┐
│          ▼                   Business Layer                          │
│  ┌──────────────────┐  ┌──────────────────┐ ┌───────────────────┐  │
│  │   PostService    │  │  AuthService     │ │  ContentService   │  │
│  │ (CRUD + 태그 관리)│  │ (로그인 + 세션)  │ │ (JSON 콘텐츠 조회)│  │
│  └────────┬─────────┘  └─────────────────-┘ └───────────────────┘  │
│  ┌────────┴─────────┐  ┌──────────────────┐                        │
│  │   PostMapper     │  │ CategoryMapper   │  (MapStruct DTO 변환)  │
│  └────────┬─────────┘  └─────────────────-┘                        │
│  ┌──────────────────┐                                               │
│  │MarkdownConverter │  (Commonmark MD→HTML)                         │
│  └──────────────────┘                                               │
└─────────────────────────────────────────────────────────────────────┘
            │
┌───────────┼─────────────────────────────────────────────────────────┐
│           ▼                 Data Access Layer                        │
│  ┌────────────────┐  ┌────────────────────┐  ┌──────────────────┐  │
│  │PostsRepository │  │ContentsRepository  │  │CategoriesRepo    │  │
│  ├────────────────┤  ├────────────────────┤  ├──────────────────┤  │
│  │PostTagsRepo    │  │UserRepository      │  │                  │  │
│  └────────┬───────┘  └────────┬───────────┘  └────────┬─────────┘  │
└───────────┼───────────────────┼───────────────────────┼─────────────┘
            │                   │                       │
┌───────────┼───────────────────┼───────────────────────┼─────────────┐
│           ▼                   ▼                       ▼             │
│                          Database Layer                              │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                        H2 / MySQL                            │   │
│  │   users ◄── posts ──► categories   contents   posts_tags     │   │
│  └──────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                        Template Layer (Thymeleaf)                    │
│  ┌──────────────┐  ┌─────────────────┐  ┌────────────────────────┐ │
│  │layout/       │  │fragments/       │  │ Pages                  │ │
│  │ default.html │  │ navigation.html │  │ index, about, post,    │ │
│  │ (base layout │◄─┤ footer.html     │  │ my-car, photos         │ │
│  │  + 로그인 모달)│  └─────────────────┘  │ (layout:decorate 적용) │ │
│  └──────────────┘                        ├────────────────────────┤ │
│                                          │ editor, photo-upload   │ │
│                                          │ (독립 Standalone)      │ │
│                                          └────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
```

## 데이터베이스 스키마

### ERD (Entity Relationship Diagram)

```
┌─────────────────────┐       ┌─────────────────────┐       ┌─────────────────────┐
│       users         │       │        posts        │       │     categories      │
├─────────────────────┤       ├─────────────────────┤       ├─────────────────────┤
│ PK  user_seq        │◄──────│ FK  user_seq        │       │ PK  category_seq    │
│     email        UQ │       │ FK  category_seq    │──────►│     category_name UQ│
│     name         UQ │       │ PK  post_seq        │       │     slug         UQ │
│     password_hash   │       │     title           │       │     description     │
│     display_name    │       │     slug         UQ │       │ FK  parent_category │──┐
│     bio             │       │     summary         │       │     display_order   │  │
│     profile_image   │       │     content         │       │     is_active       │  │
│     github_url      │       │     markdown_content│       │     created_at      │  │
│     linkedin_url    │       │     thumbnail_url   │       │     updated_at      │  │
│     is_active       │       │     status (ENUM)   │       └─────────────────────┘  │
│     created_at      │       │     view_count      │              ▲                 │
│     updated_at      │       │     published_at    │              │ Self-reference  │
└─────────────────────┘       │     created_at      │              └─────────────────┘
                              │     updated_at      │
                              └──────────┬──────────┘
                                         │
                              ┌──────────┼──────────┐
                              │          │          │
                              │          ▼          │
                              │     posts_tags      │
                              ├─────────────────────┤
                              │ PK  post_seq (FK)   │
                              │ PK  tag_seq         │
                              │     tag             │
                              │     created_at      │
                              └─────────────────────┘

┌─────────────────────┐
│      contents       │
├─────────────────────┤
│ PK  content_seq     │
│     content_type    │  (ENUM: PROFILE, CAR)
│     content (JSON)  │
│     created_at      │
│     updated_at      │
└─────────────────────┘
```

### users 테이블

사용자 계정 및 프로필 정보를 관리합니다. Spring Security 인증에 활용됩니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| `user_seq` | BIGINT | PK, AUTO_INCREMENT | 사용자 고유 식별자 |
| `email` | VARCHAR(100) | UNIQUE, NOT NULL | 이메일 (로그인 ID) |
| `name` | VARCHAR(50) | UNIQUE, NOT NULL | 사용자명 |
| `password_hash` | VARCHAR(255) | NOT NULL | BCrypt 암호화 비밀번호 |
| `display_name` | VARCHAR(100) | NOT NULL | 화면 표시 이름 |
| `bio` | TEXT | | 자기소개 |
| `profile_image_url` | VARCHAR(500) | | 프로필 이미지 URL |
| `github_url` | VARCHAR(200) | | GitHub 프로필 링크 |
| `linkedin_url` | VARCHAR(200) | | LinkedIn 프로필 링크 |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT true | Soft delete 플래그 |
| `created_at` | DATETIME | NOT NULL | 생성 일시 |
| `updated_at` | DATETIME | NOT NULL | 수정 일시 |

**인덱스**: `idx_email(email)`, `idx_name(name)`

### categories 테이블

게시글 분류를 위한 계층형 카테고리입니다. Self-referencing FK로 무한 depth 지원합니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| `category_seq` | BIGINT | PK, AUTO_INCREMENT | 카테고리 고유 식별자 |
| `category_name` | VARCHAR(100) | UNIQUE, NOT NULL | 카테고리명 |
| `slug` | VARCHAR(100) | UNIQUE, NOT NULL | URL용 슬러그 |
| `description` | TEXT | | 카테고리 설명 |
| `parent_category_id` | BIGINT | FK (self) | 상위 카테고리 ID |
| `display_order` | INT | NOT NULL, DEFAULT 0 | 정렬 순서 |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT true | Soft delete 플래그 |
| `created_at` | DATETIME | NOT NULL | 생성 일시 |
| `updated_at` | DATETIME | NOT NULL | 수정 일시 |

**인덱스**: `idx_slug(slug)`, `idx_parent(parent_category_id)`

### posts 테이블

블로그 게시글의 핵심 테이블입니다. HTML 변환본(`content`)과 Markdown 원본(`markdown_content`)을 모두 보관합니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| `post_seq` | BIGINT | PK, AUTO_INCREMENT | 게시글 고유 식별자 |
| `user_seq` | BIGINT | FK, NOT NULL | 작성자 ID |
| `category_seq` | BIGINT | FK | 카테고리 ID |
| `title` | VARCHAR(200) | NOT NULL | 제목 |
| `slug` | VARCHAR(200) | UNIQUE, NOT NULL | URL용 슬러그 |
| `summary` | TEXT | | 요약 (미리보기용) |
| `content` | LONGTEXT | NOT NULL | 본문 (변환된 HTML) |
| `markdown_content` | LONGTEXT | | 본문 (Markdown 원본) |
| `thumbnail_url` | VARCHAR(500) | | 썸네일 이미지 URL |
| `status` | ENUM | NOT NULL | 게시 상태 |
| `view_count` | INT | NOT NULL, DEFAULT 0 | 조회수 |
| `published_at` | DATETIME | | 발행 일시 |
| `created_at` | DATETIME | NOT NULL | 생성 일시 |
| `updated_at` | DATETIME | NOT NULL | 수정 일시 |

**인덱스**: `idx_slug`, `idx_status`, `idx_published_at`, `idx_user_seq`, `idx_category_seq`

### posts_tags 테이블

게시글과 태그의 다대다(M:N) 관계를 관리하는 조인 테이블입니다. 복합키(`@IdClass`)를 사용합니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| `post_seq` | BIGINT | PK, FK | 게시글 ID |
| `tag_seq` | BIGINT | PK | 태그 ID |
| `tag` | VARCHAR(100) | NOT NULL | 태그명 |
| `created_at` | DATETIME | NOT NULL | 생성 일시 |

### contents 테이블

다양한 유형의 콘텐츠를 JSON 형태로 유연하게 저장합니다. Kotlin data class로 구현되었습니다.

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| `content_seq` | BIGINT | PK, AUTO_INCREMENT | 콘텐츠 고유 식별자 |
| `content_type` | ENUM | NOT NULL | 콘텐츠 유형 |
| `content` | JSON | NOT NULL | 콘텐츠 데이터 (Key-Value) |
| `created_at` | DATETIME | NOT NULL | 생성 일시 |
| `updated_at` | DATETIME | | 수정 일시 |

**인덱스**: `idx_content_type(content_type)`

### PostStatus ENUM

| 값 | 설명 |
|----|------|
| `DRAFT` | 초안 (발행 대기) |
| `PUBLISHED` | 발행됨 |
| `ARCHIVED` | 보관됨 (비공개) |

### ContentType ENUM

| 값 | 설명 |
|----|------|
| `PROFILE` | 개발자 자기소개 |
| `CAR` | 보유 자동차 소개 |

### 설계 특징

- **Soft Delete**: `is_active` 플래그로 논리적 삭제 구현 (데이터 복구 가능)
- **Slug 지원**: SEO 친화적 URL (`/post/my-first-blog-post`)
- **타임스탬프 자동화**: Hibernate `@CreationTimestamp`, `@UpdateTimestamp` 활용
- **계층형 카테고리**: Self-referencing FK로 트리 구조 지원
- **JSON 컬럼**: `contents` 테이블에서 `@JdbcTypeCode(SqlTypes.JSON)`으로 유연한 데이터 저장
- **복합키 (Composite Key)**: `posts_tags` 테이블에서 `@IdClass`를 활용한 M:N 관계 매핑
- **Markdown 원본 보존**: `markdown_content` 컬럼에 원본 저장, `content`에 HTML 변환본 저장
- **인덱스 최적화**: 자주 조회되는 컬럼에 인덱스 설정
- **검색 엔진 차단**: `robots.txt`로 크롤링 차단 (개발/포트폴리오 단계)

## 시작하기

### 요구 사항
- JDK 21 이상

### 프로젝트 빌드

```bash
./gradlew build
```

### 애플리케이션 실행

```bash
./gradlew bootRun
```

실행 후 http://localhost:8080 으로 접속하세요.

### 테스트 실행

```bash
# 전체 테스트
./gradlew test

# 단일 테스트
./gradlew test --tests "com.walter.lifelog.SomeTest"
```

## API 문서 및 모니터링

| 용도 | URL |
|------|-----|
| **Swagger UI** | http://localhost:8080/swagger-ui.html |
| **OpenAPI JSON** | http://localhost:8080/v3/api-docs |
| **Health Check** | http://localhost:8080/actuator/health |
| **Prometheus Metrics** | http://localhost:8080/actuator/prometheus |
| **H2 Console** (개발) | http://localhost:8080/h2-console |

### H2 Console 접속 정보
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (비워두기)

## 페이지 및 API 엔드포인트

### SSR 페이지 (서버 사이드 렌더링)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/`, `/index` | 메인 홈페이지 |
| GET | `/post` | 게시글 페이지 |
| GET | `/post/editor` | 게시글 에디터 (신규 작성) |
| GET | `/post/editor/{postSeq}` | 게시글 에디터 (수정) |
| GET | `/about` | 자기소개 페이지 |
| GET | `/my-car` | 애차 소개 페이지 |
| GET | `/photos` | 사진 갤러리 |
| GET | `/photos/upload` | 사진 업로드 |

### REST API — 게시글

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/post/{inquiryStr}` | 게시글 조회 (ID 또는 Slug) |
| POST | `/api/post/save` | 게시글 저장 (로그인 세션 필요) |

### REST API — 인증

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/auth/login` | 관리자 로그인 (세션 생성) |
| POST | `/api/auth/logout` | 로그아웃 (세션 무효화) |
| GET | `/api/auth/status` | 로그인 상태 확인 |

### 공통 응답 형식 (`Rest<T>`)

```json
{
  "timestamp": "2026-03-01T14:00:00",
  "statusCode": 200,
  "isSuccess": true,
  "message": "OK",
  "data": { ... }
}
```

## 프로젝트 구조

```
src/
├── main/
│   ├── kotlin/com/walter/lifelog/
│   │   ├── LifelogApplication.kt          # 애플리케이션 진입점
│   │   ├── annotation/
│   │   │   └── Facade.kt                  # @Facade 커스텀 애노테이션
│   │   ├── config/
│   │   │   ├── SecurityConfig.kt          # Spring Security 설정
│   │   │   ├── SwaggerConfig.kt           # OpenAPI/Swagger 설정
│   │   │   ├── GlobalExceptionHandler.kt  # 전역 예외 처리
│   │   │   └── exception/
│   │   │       └── PostNotFoundException.kt
│   │   ├── controller/
│   │   │   ├── AuthController.kt          # 인증 REST API
│   │   │   ├── PostController.kt          # 게시글 REST API
│   │   │   └── dto/                       # DTO 클래스
│   │   │       ├── LoginRequest.kt
│   │   │       ├── LoginResponse.kt
│   │   │       ├── LoginStatusResponse.kt
│   │   │       ├── PostCategory.kt
│   │   │       ├── PostRequest.kt
│   │   │       ├── PostResponse.kt
│   │   │       ├── PostSaveResponse.kt
│   │   │       └── Rest.kt               # 공통 API 응답 래퍼
│   │   ├── entity/
│   │   │   ├── Post.kt
│   │   │   ├── PostTag.kt
│   │   │   ├── Content.kt
│   │   │   ├── Category.kt
│   │   │   ├── User.kt
│   │   │   └── code/
│   │   │       ├── PostStatus.kt
│   │   │       └── ContentType.kt
│   │   ├── mapper/
│   │   │   ├── PostMapper.kt             # Post ↔ DTO 변환
│   │   │   └── CategoryMapper.kt         # Category → PostCategory 변환
│   │   ├── repository/
│   │   │   ├── PostsRepository.kt
│   │   │   ├── PostTagsRepository.kt
│   │   │   ├── ContentsRepository.kt
│   │   │   ├── CategoriesRepository.kt
│   │   │   └── UserRepository.kt
│   │   └── service/
│   │       ├── PostService.kt             # 게시글 CRUD + 태그 관리
│   │       ├── AuthService.kt             # 로그인 인증 + 세션 관리
│   │       ├── ContentService.kt          # JSON 콘텐츠 조회
│   │       └── CustomUserDetailsService.kt # Spring Security UserDetails
│   ├── java/com/walter/lifelog/
│   │   ├── controller/
│   │   │   ├── RenderingController.java   # 정적 SSR 페이지 렌더링
│   │   │   ├── ContentController.java     # 콘텐츠 SSR (about, my-car)
│   │   │   └── PostViewController.java    # 에디터 SSR (카테고리 로드)
│   │   └── util/
│   │       └── MarkdownConverter.java     # Markdown → HTML 변환
│   └── resources/
│       ├── application.yml                # 개발 환경 설정
│       ├── application-live.yml           # 운영 환경 설정
│       ├── static/
│       │   ├── css/
│       │   │   ├── lifelog.css            # 공통 스타일
│       │   │   ├── post.css
│       │   │   ├── editor.css
│       │   │   ├── my-car.css
│       │   │   ├── login.css              # 로그인 모달 스타일
│       │   │   └── photo-upload.css
│       │   ├── js/
│       │   │   └── editor.js              # 에디터 JavaScript
│       │   └── robots.txt                 # 검색 엔진 크롤링 차단
│       └── templates/
│           ├── layout/
│           │   └── default.html           # 베이스 레이아웃 (nav + footer + 로그인 모달)
│           ├── fragments/
│           │   ├── navigation.html        # 공통 네비게이션 프래그먼트
│           │   └── footer.html            # 공통 푸터 프래그먼트
│           ├── index.html                 # 메인 (layout:decorate)
│           ├── post.html                  # 게시글 (layout:decorate)
│           ├── about.html                 # 자기소개 (layout:decorate)
│           ├── my-car.html                # 애차 소개 (layout:decorate)
│           ├── photos.html                # 사진 갤러리 (layout:decorate)
│           ├── editor.html                # 게시글 에디터 (Standalone)
│           └── photo-upload.html          # 사진 업로드 (Standalone)
└── test/
    ├── kotlin/com/walter/lifelog/
    │   ├── LifelogApplicationTests.kt
    │   ├── mapper/
    │   │   └── PostMapperTest.kt
    │   ├── service/
    │   │   └── PostServiceTest.kt
    │   └── util/
    │       └── PasswordEncoderTest.kt
    └── java/com/walter/lifelog/util/
        └── MarkdownConverterTest.java
```

## 프로필 설정

```bash
# 개발 환경 (H2 인메모리 DB)
./gradlew bootRun

# 운영 환경 (MySQL)
./gradlew bootRun --args='--spring.profiles.active=live'
```

## 라이선스

MIT License

---

## 문서 업데이트 이력

| 날짜 | 내용 |
|------|------|
| 2026-03-02 | Spring Security 인증 추가, Post 저장 API, Markdown 원본 보존, Thymeleaf Layout Dialect 적용, 에디터 JS 분리, 로그인 모달, 공통 응답 형식(`Rest<T>`), Java→Kotlin 전환(DTO/Repository/Service), 테스트 코드 추가 |
| 2026-02-28 | Thymeleaf 전환 반영, PostTag 테이블 추가, Content 엔티티 Kotlin 전환, DB 드라이버 MySQL 변경, Commonmark 추가 |
| 2026-02-24 | Content 도메인(JSON 엔티티, ContentType) 추가, SSR 페이지 엔드포인트 정리, 아키텍처 다이어그램 갱신 |
| 2026-02-19 | 포트폴리오용 리팩터링, DB 스키마 상세 문서화, 아키텍처 다이어그램 추가 |
| 2026-02-03 | 초기 README 작성 |
