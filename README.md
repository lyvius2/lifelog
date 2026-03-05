# Lifelog

> 개인 포트폴리오 프로젝트 — 일상을 기록하는 블로그/라이프로그 플랫폼

Spring Boot 4.0과 Kotlin/Java를 활용한 풀스택 웹 애플리케이션입니다.
RESTful API 설계, JPA 기반 데이터 모델링, Spring Security 인증을 적용했으며,
**MSA 전환을 고려한 Modular Monolith Architecture**로 설계되었습니다.

## 아키텍처 개요

### Modular Monolith을 선택한 이유

이 프로젝트는 포트폴리오 목적으로 클라우드 서버(Vultr, 2 Core CPU / 4GB RAM)에 배포됩니다.
제한된 인프라 스펙에서 다수의 서비스 인스턴스, 서비스 디스커버리, API Gateway 등을 운용하는 MSA를 직접 구현하기 어렵기 때문에,
단일 프로세스로 배포 가능한 Monolithic 구조를 유지하되 **도메인과 기능별로 모듈을 분리**하여 **Modular Monolith Architecture**로 전환했습니다.

이 접근 방식의 장점은 다음과 같습니다.

- **단일 배포 단위**: 하나의 JAR로 빌드·배포되므로 제한된 서버 환경에서도 운용이 가능합니다.
- **모듈 간 경계 명확화**: 각 도메인(사용자, 블로그, 콘텐츠 등)이 독립된 Gradle 모듈로 분리되어 응집도가 높고 결합도가 낮습니다.
- **MSA 전환 용이성**: 모듈 간 의존성이 명시적으로 관리되어, 향후 인프라가 확보되면 개별 모듈을 독립 서비스로 분리할 수 있습니다.
- **빌드 효율성**: 변경된 모듈만 증분 빌드되어 개발 속도가 향상됩니다.

### 모듈 구조

```
lifelog/
├── app/                        ← 애플리케이션 부트스트랩 (유일한 실행 모듈)
├── web/                        ← SSR 프레젠테이션 계층 (Thymeleaf)
├── api/                        ← REST API 프레젠테이션 계층 (Swagger)
├── user-service/               ← 사용자·인증 도메인
├── blog-service/               ← 블로그(게시글·카테고리·태그) 도메인
├── content-service/            ← 콘텐츠(프로필·차량 소개 등) 도메인
├── photo-archive-service/      ← 사진 아카이브 도메인 (개발 예정)
└── shared/                     ← 공통 유틸리티·설정
```

### 모듈 의존성

```
                         ┌──────────────────┐
                         │       app        │
                         │  (Bootstrap)     │
                         └───────┬──────────┘
                           ┌─────┴─────┐
                           ▼           ▼
                    ┌──────────┐ ┌──────────┐
                    │   web    │ │   api    │
                    │  (SSR)   │ │  (REST)  │
                    └────┬─────┘ └────┬─────┘
                         │            │
          ┌──────────────┼────────────┼──────────────┐
          ▼              ▼            ▼              ▼
  ┌──────────────┐ ┌───────────┐ ┌────────────┐ ┌────────────────────┐
  │ user-service │ │blog-service│ │content-svc │ │photo-archive-svc   │
  │  (사용자)     │ │  (블로그)  │ │ (콘텐츠)   │ │  (사진, 개발 예정)  │
  └──────┬───────┘ └─────┬─────┘ └─────┬──────┘ └─────────┬──────────┘
         │               │             │                   │
         ▼               ▼             ▼                   ▼
  ┌─────────────────────────────────────────────────────────────────┐
  │                          shared                                 │
  │              (공통 유틸리티·설정·어노테이션)                       │
  └─────────────────────────────────────────────────────────────────┘
```

> 각 도메인 서비스 모듈은 `shared`에만 의존하며, 서비스 모듈 간에는 직접 의존성이 없습니다.
> `web`과 `api`가 모든 서비스 모듈을 조합하여 프레젠테이션 계층에서 오케스트레이션합니다.

### 각 모듈의 역할

| 모듈 | 유형 | 역할 |
|------|------|------|
| **app** | Bootstrap | Spring Boot 애플리케이션 진입점. 전역 Security 필터 설정, `bootJar`를 생성하는 유일한 실행 모듈. `web`과 `api`만 의존 |
| **web** | Presentation | Thymeleaf 기반 SSR 컨트롤러. 메인 페이지, 게시글 뷰, 에디터, 프로필, 사진 갤러리 등 화면 렌더링 |
| **api** | Presentation | REST API 컨트롤러. 게시글 CRUD, 인증, 카테고리 조회 API. Swagger UI 문서화 |
| **user-service** | Domain | 사용자 엔티티·인증 로직. Spring Security UserDetailsService, BCrypt, 세션/JWT 이중 인증 |
| **blog-service** | Domain | 블로그 핵심 도메인. 게시글·카테고리·태그 CRUD, MapStruct DTO 변환. `shared`에만 의존 |
| **content-service** | Domain | JSON 기반 유연한 콘텐츠 관리. 자기소개(PROFILE), 애차 소개(CAR) 등 타입별 콘텐츠 저장·조회 |
| **photo-archive-service** | Domain | 사진 아카이브 기능 (개발 예정) |
| **shared** | Infrastructure | 모든 도메인 모듈이 공유하는 유틸리티. JWT 토큰 핸들러, Markdown 변환기, Virtual Thread 설정, @Facade 어노테이션, 공통 예외 클래스 |

## 주요 기능

- **게시글 관리**: CRUD 및 ID/Slug 기반 조회, Markdown 원본 보존
- **Markdown 에디터**: Commonmark 기반 Markdown → HTML 변환, 에디터 UI에서 카테고리·태그 입력 지원
- **관리자 인증**: Spring Security + 세션/JWT 이중 인증, BCrypt 암호화, 24시간 유효 Access Token
- **카테고리 시스템**: 계층 구조(Self-referencing) 카테고리, 최대 3 depth 트리 조회
- **태그 시스템**: 게시글에 태그를 부여하는 다대다(M:N) 관계
- **콘텐츠 관리**: JSON 기반 유연한 콘텐츠 저장 (자기소개, 애차 소개 등)
- **사진 갤러리**: 이미지 갤러리 뷰 및 업로드
- **SSR 페이지**: Thymeleaf 템플릿 기반 서버 사이드 렌더링
- **레이아웃 데코레이터 패턴**: Thymeleaf Layout Dialect로 공통 nav/footer 분리
- **반응형 UI**: 네비게이션, 푸터 포함 모바일/데스크톱 대응
- **Facade 패턴**: Controller → Facade → Service 구조로 비즈니스 오케스트레이션 분리
- **Virtual Thread**: Java 21 Virtual Thread로 블로그 조회 시 이전/다음 글·작성자 정보 병렬 처리
- **공통 API 응답 형식**: `Rest<T>` 제네릭 래퍼로 일관된 JSON 응답 구조
- **API 문서화**: Swagger UI 자동 생성

## 기술 스택

| 구분 | 기술 |
|------|------|
| **Language** | Kotlin 2.2.21, Java 21 |
| **Framework** | Spring Boot 4.0.2 |
| **Architecture** | Multi-Module Modular Monolith (Gradle) |
| **Security** | Spring Security (세션 + JWT 이중 인증, BCrypt, CSRF 비활성) |
| **JWT** | JJWT 0.12.6 (Access Token 생성/검증, HMAC-SHA256 서명) |
| **ORM** | Spring Data JPA, Hibernate |
| **Template Engine** | Thymeleaf (SSR) + Thymeleaf Layout Dialect (Decorator Pattern) |
| **Markdown** | Commonmark 0.24.0 (GFM Tables, Strikethrough, Autolink, Heading Anchor, Task List) |
| **Database** | H2 (개발), MySQL (운영) |
| **Build Tool** | Gradle 9.3.0 (Kotlin DSL, Multi-Module) |
| **API Documentation** | Springdoc OpenAPI 2.7.0 (Swagger) |
| **Object Mapping** | MapStruct 1.6.3 |
| **Concurrency** | Java 21 Virtual Thread |
| **Monitoring** | Spring Boot Actuator |

## 레이어드 아키텍처

각 모듈은 레이어드 아키텍처를 따르며, 모듈 간 통신은 직접 메서드 호출로 이루어집니다.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Presentation Layer                                  │
│                                                                             │
│  ┌─ web module ──────────────────────┐  ┌─ api module ────────────────────┐ │
│  │ RenderingController  (SSR Pages)  │  │ PostController     (REST API)  │ │
│  │ ContentController    (SSR Pages)  │  │ AuthController     (REST API)  │ │
│  │ PostViewController   (SSR Editor) │  │ CategoryController (REST API)  │ │
│  └───────────────────────────────────┘  │ SwaggerConfig                  │ │
│                                         │ Rest<T>           (공통 응답)   │ │
│                                         └────────────────────────────────┘ │
└──────────────┬──────────────────────────────────────┬──────────────────────┘
               │                                      │
┌──────────────┼──────────────────────────────────────┼──────────────────────┐
│              ▼              Facade Layer             ▼                      │
│  ┌─ blog-service ─────────────────────────────────────────────────────┐    │
│  │  PostFacade (@Facade)                                              │    │
│  │  - 게시글 조회·저장 오케스트레이션                                    │    │
│  │  - 에디터 데이터 조합 (카테고리 + 게시글 + 태그)                     │    │
│  │  - Virtual Thread로 이전/다음 글 병렬 조회                          │    │
│  └────────────────────────────────────────────────────────────────────┘    │
└───────────────────────────────────────────────────────────────────────────-┘
               │
┌──────────────┼────────────────────────────────────────────────────────────-┐
│              ▼              Business Layer (Domain Modules)                 │
│                                                                             │
│  ┌─ blog-service ───────┐  ┌─ user-service ───────┐  ┌─ content-service ┐ │
│  │ PostService    (CRUD) │  │ AuthService  (인증)   │  │ ContentService   │ │
│  │ CategoryService      │  │ UserService  (조회)   │  │  (JSON 콘텐츠)   │ │
│  │ PostTagService       │  │ CustomUserDetails     │  │                  │ │
│  │ PostMapper (MapStruct)│  │ UserMapper (MapStruct)│  └──────────────────┘ │
│  │ CategoryMapper       │  │ SecurityConfig        │                       │
│  └──────────────────────┘  └──────────────────────┘                        │
│                                                                             │
│  ┌─ shared ─────────────────────────────────────────────────────────────┐  │
│  │ AccessTokenHandler (JWT)  │ MarkdownConverter (Commonmark MD→HTML)   │  │
│  │ VirtualThreadConfig       │ @Facade 어노테이션                        │  │
│  │ PostNotFoundException     │                                           │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────────────────────-┘
               │
┌──────────────┼────────────────────────────────────────────────────────────-┐
│              ▼              Data Access Layer                               │
│  ┌────────────────┐  ┌────────────────────┐  ┌──────────────────────────┐ │
│  │PostsRepository │  │ContentsRepository  │  │CategoriesRepository      │ │
│  │PostTagsRepo    │  │UserRepository      │  │                          │ │
│  └────────┬───────┘  └────────┬───────────┘  └────────┬─────────────────┘ │
└───────────┼───────────────────┼───────────────────────┼───────────────────┘
            │                   │                       │
┌───────────┼───────────────────┼───────────────────────┼───────────────────┐
│           ▼                   ▼                       ▼                   │
│                          Database Layer                                    │
│  ┌──────────────────────────────────────────────────────────────────────┐ │
│  │                        H2 / MySQL                                    │ │
│  │   users ◄── posts ──► categories   contents   posts_tags             │ │
│  └──────────────────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                        Template Layer (Thymeleaf)                            │
│  ┌──────────────┐  ┌─────────────────┐  ┌──────────────────────────────┐   │
│  │layout/       │  │fragments/       │  │ Pages                        │   │
│  │ default.html │  │ navigation.html │  │ index, about, post,          │   │
│  │ (base layout │◄─┤ footer.html     │  │ my-car, photos               │   │
│  │  + 로그인 모달)│  └─────────────────┘  │ (layout:decorate 적용)       │   │
│  └──────────────┘                        ├──────────────────────────────┤   │
│                                          │ editor, photo-upload         │   │
│                                          │ (독립 Standalone)            │   │
│                                          └──────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 프로젝트 구조

```
lifelog/
├── build.gradle.kts                   # 루트: 플러그인 버전 관리 + subprojects 공통 설정
├── settings.gradle.kts                # 모듈 include 선언
│
├── app/                               # [Bootstrap Module]
│   └── src/main/kotlin/
│       └── com/walter/lifelog/
│           ├── LifelogApplication.kt  #   Spring Boot 메인 클래스
│           └── config/
│               └── FilterConfig.kt    #   전역 Security 필터 설정
│
├── web/                               # [Web Presentation Module]
│   └── src/main/java/
│       └── com/walter/lifelog/web/controller/
│           ├── RenderingController.java   # 정적 SSR 페이지 (홈, 사진)
│           ├── ContentController.java     # 콘텐츠 SSR (about, my-car)
│           └── PostViewController.java    # 게시글 상세·에디터 SSR
│
├── api/                               # [API Presentation Module]
│   └── src/main/kotlin/
│       └── com/walter/lifelog/api/
│           ├── config/
│           │   └── SwaggerConfig.kt       # OpenAPI/Swagger 설정
│           └── controller/
│               ├── AuthController.kt      # 인증 REST API
│               ├── PostController.kt      # 게시글 REST API
│               ├── CategoryController.kt  # 카테고리 REST API
│               └── dto/
│                   └── Rest.kt            # 공통 API 응답 래퍼
│
├── user-service/                      # [User Domain Module]
│   └── src/main/kotlin/
│       └── com/walter/lifelog/user/
│           ├── config/
│           │   └── SecurityConfig.kt      # AuthenticationManager, BCrypt 설정
│           ├── entity/
│           │   └── User.kt               # 사용자 엔티티
│           ├── dto/
│           │   ├── LoginRequest.kt
│           │   ├── LoginResponse.kt
│           │   ├── LoginStatusResponse.kt
│           │   └── Author.kt
│           ├── mapper/
│           │   └── UserMapper.kt          # MapStruct: User → Author 변환
│           ├── repository/
│           │   └── UserRepository.kt
│           └── service/
│               ├── AuthService.kt             # 로그인 + JWT 발급
│               ├── CustomUserDetailsService.kt # Spring Security UserDetails
│               └── UserService.kt             # 사용자 조회
│
├── blog-service/                      # [Blog Domain Module]
│   └── src/main/kotlin/
│       └── com/walter/lifelog/blog/
│           ├── entity/
│           │   ├── Post.kt                # 게시글 엔티티
│           │   ├── Category.kt            # 카테고리 엔티티 (계층 구조)
│           │   ├── PostTag.kt             # 게시글-태그 M:N 엔티티
│           │   └── code/
│           │       └── PostStatus.kt      # DRAFT / PUBLISHED / ARCHIVED
│           ├── dto/
│           │   ├── PostRequest.kt
│           │   ├── PostResponse.kt
│           │   ├── PostSaveResponse.kt
│           │   ├── PostCategory.kt
│           │   └── CategoryTreeResponse.kt
│           ├── facade/
│           │   └── PostFacade.kt          # 비즈니스 오케스트레이션 (@Facade)
│           ├── mapper/
│           │   ├── PostMapper.kt          # MapStruct: Post ↔ DTO
│           │   └── CategoryMapper.kt      # MapStruct: Category → DTO
│           ├── repository/
│           │   ├── PostsRepository.kt
│           │   ├── CategoriesRepository.kt
│           │   └── PostTagsRepository.kt
│           └── service/
│               ├── PostService.kt         # 게시글 CRUD
│               ├── CategoryService.kt     # 카테고리 트리 조회
│               └── PostTagService.kt      # 태그 관리
│
├── content-service/                   # [Content Domain Module]
│   └── src/main/kotlin/
│       └── com/walter/lifelog/content/
│           ├── entity/
│           │   ├── Content.kt             # JSON 콘텐츠 엔티티
│           │   └── code/
│           │       └── ContentType.kt     # PROFILE / CAR
│           ├── repository/
│           │   └── ContentsRepository.kt
│           └── service/
│               └── ContentService.kt      # 타입별 콘텐츠 조회
│
├── photo-archive-service/             # [Photo Archive Domain Module] (개발 예정)
│
└── shared/                            # [Shared Infrastructure Module]
    └── src/main/java/
        └── com/walter/lifelog/shared/
            ├── annotation/
            │   └── Facade.java            # @Facade 커스텀 어노테이션
            ├── config/
            │   ├── VirtualThreadConfig.java   # Virtual Thread TaskExecutor
            │   └── exception/
            │       └── PostNotFoundException.java
            └── util/
                ├── AccessTokenHandler.java    # JWT 토큰 생성/검증
                └── MarkdownConverter.java     # Markdown → HTML 변환
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

### 테이블 상세

<details>
<summary>users — 사용자 계정 및 프로필</summary>

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

인덱스: `idx_email(email)`, `idx_name(name)`

</details>

<details>
<summary>categories — 계층형 카테고리</summary>

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

인덱스: `idx_slug(slug)`, `idx_parent(parent_category_id)`

</details>

<details>
<summary>posts — 블로그 게시글</summary>

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
| `status` | ENUM | NOT NULL | DRAFT / PUBLISHED / ARCHIVED |
| `view_count` | INT | NOT NULL, DEFAULT 0 | 조회수 |
| `published_at` | DATETIME | | 발행 일시 |
| `created_at` | DATETIME | NOT NULL | 생성 일시 |
| `updated_at` | DATETIME | NOT NULL | 수정 일시 |

인덱스: `idx_slug`, `idx_status`, `idx_published_at`, `idx_user_seq`, `idx_category_seq`

</details>

<details>
<summary>posts_tags — 게시글-태그 M:N</summary>

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| `post_seq` | BIGINT | PK, FK | 게시글 ID |
| `tag_seq` | BIGINT | PK | 태그 ID |
| `tag` | VARCHAR(100) | NOT NULL | 태그명 |
| `created_at` | DATETIME | NOT NULL | 생성 일시 |

</details>

<details>
<summary>contents — JSON 콘텐츠</summary>

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| `content_seq` | BIGINT | PK, AUTO_INCREMENT | 콘텐츠 고유 식별자 |
| `content_type` | ENUM | NOT NULL | PROFILE / CAR |
| `content` | JSON | NOT NULL | 콘텐츠 데이터 (Key-Value) |
| `created_at` | DATETIME | NOT NULL | 생성 일시 |
| `updated_at` | DATETIME | | 수정 일시 |

인덱스: `idx_content_type(content_type)`

</details>

### 설계 특징

- **Soft Delete**: `is_active` 플래그로 논리적 삭제 구현
- **Slug 지원**: SEO 친화적 URL (`/post/my-first-blog-post`)
- **타임스탬프 자동화**: Hibernate `@CreationTimestamp`, `@UpdateTimestamp` 활용
- **계층형 카테고리**: Self-referencing FK로 트리 구조 지원
- **JSON 컬럼**: `contents` 테이블에서 `@JdbcTypeCode(SqlTypes.JSON)`으로 유연한 데이터 저장
- **복합키**: `posts_tags` 테이블에서 `@IdClass`를 활용한 M:N 관계 매핑
- **Markdown 원본 보존**: `markdown_content`에 원본 저장, `content`에 HTML 변환본 저장
- **이중 인증**: 세션 기반 인증과 JWT Bearer Token 인증을 동시 지원
- **JWT 서명 안전성**: 짧은 시크릿 키도 SHA-256 해싱으로 256-bit HMAC 키를 보장

## 시작하기

### 요구 사항
- JDK 21 이상

### 프로젝트 빌드

```bash
./gradlew build
```

### 애플리케이션 실행

```bash
# 개발 환경 (H2 인메모리 DB)
./gradlew :app:bootRun

# 운영 환경 (MySQL)
./gradlew :app:bootRun --args='--spring.profiles.active=live'
```

실행 후 http://localhost:8080 으로 접속하세요.

### 테스트 실행

```bash
# 전체 테스트
./gradlew test

# 특정 모듈 테스트
./gradlew :blog-service:test
./gradlew :user-service:test
```

## API 문서 및 모니터링

| 용도 | URL |
|------|-----|
| **Swagger UI** | http://localhost:8080/swagger-ui.html |
| **OpenAPI JSON** | http://localhost:8080/v3/api-docs |
| **Health Check** | http://localhost:8080/actuator/health |
| **H2 Console** (개발) | http://localhost:8080/h2-console |

## 페이지 및 API 엔드포인트

### SSR 페이지 (web 모듈)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/`, `/index` | 메인 홈페이지 |
| GET | `/post/{inquiryStr}` | 게시글 상세 (ID 또는 Slug) |
| GET | `/post/editor` | 게시글 에디터 (신규 작성) |
| GET | `/post/editor/{postSeq}` | 게시글 에디터 (수정) |
| GET | `/about` | 자기소개 페이지 |
| GET | `/my-car` | 애차 소개 페이지 |
| GET | `/photos` | 사진 갤러리 |
| GET | `/photos/upload` | 사진 업로드 |

### REST API (api 모듈)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/auth/login` | 관리자 로그인 (세션 생성 + Access Token 발급) |
| GET | `/api/auth/status` | 로그인 상태 확인 |
| GET | `/api/post/{inquiryStr}` | 게시글 조회 (ID 또는 Slug) |
| POST | `/api/post/save` | 게시글 저장 (Bearer Token 또는 세션 인증) |
| GET | `/api/category/tree` | 카테고리 트리 조회 (최대 3 depth) |

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

### 로그인 응답

```json
{
  "success": true,
  "message": "로그인 성공",
  "displayName": "관리자",
  "accessToken": "eyJhbGciOiJIUzI1...",
  "expire": 1440
}
```

> `expire`는 Access Token 만료 시간 (분 단위, 24시간 = 1440분)

## 라이선스

MIT License

---

## 문서 업데이트 이력

| 날짜 | 내용 |
|------|------|
| 2026-03-06 | 서비스 모듈 간 의존성 제거: 도메인 모듈은 `shared`에만 의존하도록 개선, `web`/`api`가 서비스를 조합하는 구조로 변경, 모듈 의존성 다이어그램·역할 설명 갱신 |
| 2026-03-05 | Modular Monolith Architecture 전환 반영: 8개 Gradle 서브모듈 구조 문서화, 모듈별 역할·의존성 다이어그램 추가, 아키텍처 채택 사유 기술, 미사용 의존성(Spring Cloud, WebFlux, Reactor) 정리, 기술 스택 갱신 |
| 2026-03-02 | JWT Access Token 인증 추가(JJWT), Facade 패턴 적용(`PostFacade`), Service 계층 분리(`CategoryService`, `PostTagService`, `UserService`), Bearer Token/세션 이중 인증, `LoginResponse`에 `accessToken`·`expire` 필드 추가, `AccessTokenHandlerTest` 추가 |
| 2026-03-02 | Spring Security 인증 추가, Post 저장 API, Markdown 원본 보존, Thymeleaf Layout Dialect 적용, 에디터 JS 분리, 로그인 모달, 공통 응답 형식(`Rest<T>`), Java→Kotlin 전환(DTO/Repository/Service), 테스트 코드 추가 |
| 2026-02-28 | Thymeleaf 전환 반영, PostTag 테이블 추가, Content 엔티티 Kotlin 전환, DB 드라이버 MySQL 변경, Commonmark 추가 |
| 2026-02-24 | Content 도메인(JSON 엔티티, ContentType) 추가, SSR 페이지 엔드포인트 정리, 아키텍처 다이어그램 갱신 |
| 2026-02-19 | 포트폴리오용 리팩터링, DB 스키마 상세 문서화, 아키텍처 다이어그램 추가 |
| 2026-02-03 | 초기 README 작성 |
