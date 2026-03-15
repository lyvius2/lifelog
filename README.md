# Lifelog

> 개인 포트폴리오 프로젝트 — 일상을 기록하는 블로그/라이프로그 플랫폼

Spring Boot 4.0과 Kotlin/Java를 활용한 풀스택 웹 애플리케이션입니다.
RESTful API 설계, JPA 기반 데이터 모델링, Spring Security 인증을 적용했으며,
**MSA 전환을 고려한 Modular Monolith Architecture**로 설계되었습니다.

> **Frontend Note**: 프런트엔드(Thymeleaf 템플릿, CSS, JavaScript)는 **Vibe Coding**을 주로 활용하여 개발했습니다.
> 백엔드 아키텍처와 비즈니스 로직에 집중하되, UI/UX 구현은 AI 코딩 어시스턴트와의 협업으로 빠르게 프로토타이핑했습니다.

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
  │  (사용자)     │ │  (블로그)  │ │ (콘텐츠)   │ │  (사진 아카이브)    │
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
| **api** | Presentation | REST API 컨트롤러 + AuthFacade. 게시글 CRUD/검색, 인증(RSA 공개키), 카테고리 조회 API. Swagger UI 문서화 |
| **user-service** | Domain | 사용자 엔티티·인증 로직. Spring Security UserDetailsService, BCrypt, 세션/JWT 이중 인증 |
| **blog-service** | Domain | 블로그 핵심 도메인. 게시글·카테고리·태그 CRUD, jOOQ 동적 검색·페이징, MapStruct DTO 변환 |
| **content-service** | Domain | MongoDB Document 기반 콘텐츠 관리. 자기소개(PROFILE), 애차 소개(CAR) 등 타입별 콘텐츠 저장·조회 |
| **photo-archive-service** | Domain | 사진 아카이브 도메인. Google Drive 연동 이미지 업로드·서빙, 썸네일 자동 생성, EXIF 메타데이터 저장, jOOQ 동적 검색, PhotoArchiveFacade |
| **shared** | Infrastructure | 공통 유틸리티. JWT 토큰 핸들러, RSA 키 관리, Markdown 변환기, Google Drive 설정/헬퍼, jOOQ 공통 설정, AsyncSupporter, PageResponse, @Facade, 공통 예외 |

## 주요 기능

- **게시글 관리**: CRUD 및 ID/Slug 기반 조회, Markdown 원본 보존
- **게시글 검색·페이징**: jOOQ 기반 동적 쿼리로 키워드·카테고리·태그·상태 필터링 및 페이징 조회
- **Markdown 에디터**: Commonmark 기반 Markdown → HTML 변환, 에디터 UI에서 카테고리·태그 입력 지원
- **관리자 인증**: Spring Security + 세션/JWT 이중 인증, BCrypt 암호화, RSA 비밀번호 암호화, 24시간 유효 Access Token
- **카테고리 시스템**: 계층 구조(Self-referencing) 카테고리, 최대 3 depth 트리 조회, 인덱스 페이지 동적 카테고리 렌더링
- **태그 시스템**: 게시글에 태그를 부여하는 다대다(M:N) 관계
- **콘텐츠 관리**: MongoDB Document 기반 유연한 콘텐츠 저장 (자기소개, 애차 소개 등)
- **사진 아카이브**: Google Drive 연동 이미지 업로드·서빙, 600px 썸네일 자동 생성, 클라이언트 EXIF 추출(exifr) 및 서버 메타데이터 저장, jOOQ 동적 검색·페이징, 카테고리별 분류
- **관리자 전용 보안**: Spring Security로 에디터·업로드·Google Auth 경로 인증 보호, 비인가 접근 시 access-denied 페이지, 세션 기반 로그아웃
- **SSR 페이지**: Thymeleaf 템플릿 기반 서버 사이드 렌더링
- **레이아웃 데코레이터 패턴**: Thymeleaf Layout Dialect로 공통 nav/footer 분리
- **반응형 UI**: 네비게이션, 푸터 포함 모바일/데스크톱 대응
- **Facade 패턴**: Controller → Facade → Service 구조로 비즈니스 오케스트레이션 분리 (PostFacade, AuthFacade, PhotoArchiveFacade)
- **Virtual Thread**: Java 21 Virtual Thread로 비동기 병렬 처리 (`AsyncSupporter` 공통 유틸리티)
- **Polyglot Persistence**: RDB(MySQL/H2)와 MongoDB를 함께 사용하는 다중 데이터 소스 구성
- **DB 접속 정보 암호화**: Jasypt(PBEWithMD5AndDES)로 MySQL·MongoDB 접속 정보 암호화
- **Observability**: Prometheus 메트릭 수집, Loki 로그 수집, OpenTelemetry 분산 트레이싱, Logback 프로파일별 로깅 전략
- **공통 API 응답 형식**: `Rest<T>` 제네릭 래퍼로 일관된 JSON 응답 구조
- **API 문서화**: Swagger UI 자동 생성

## 기술 스택

| 구분 | 기술 |
|------|------|
| **Language** | Kotlin 2.2.21, Java 21 |
| **Framework** | Spring Boot 4.0.2 |
| **Architecture** | Multi-Module Modular Monolith (Gradle) |
| **Security** | Spring Security (세션 + JWT 이중 인증, BCrypt, RSA 비밀번호 암호화) |
| **JWT** | JJWT 0.12.6 (Access Token 생성/검증, HMAC-SHA256 서명) |
| **ORM** | Spring Data JPA, Hibernate |
| **Query Builder** | jOOQ (게시글 목록 동적 검색·페이징) |
| **Document DB** | MongoDB (콘텐츠 도큐먼트 저장, Spring Data MongoDB) |
| **Template Engine** | Thymeleaf (SSR) + Thymeleaf Layout Dialect (Decorator Pattern) |
| **Client Libraries** | Vanilla JS, Prism.js (코드 하이라이팅), exifr (클라이언트 EXIF 추출) |
| **Markdown** | Commonmark 0.24.0 (GFM Tables, Strikethrough, Autolink, Heading Anchor, Task List) |
| **Database** | H2 (개발 RDB), MySQL (운영 RDB), MongoDB (콘텐츠 도큐먼트) |
| **Build Tool** | Gradle 9.3.0 (Kotlin DSL, Multi-Module) |
| **API Documentation** | Springdoc OpenAPI 2.7.0 (Swagger) |
| **Object Mapping** | MapStruct 1.6.3 |
| **External Storage** | Google Drive API v3 (OAuth 2.0, 이미지 업로드·서빙·썸네일 생성) |
| **Concurrency** | Java 21 Virtual Thread, AsyncSupporter (CompletableFuture 래퍼) |
| **Encryption** | Jasypt 3.0.5 (PBEWithMD5AndDES, DB 접속 정보 암호화) |
| **Observability** | Micrometer Prometheus (메트릭), Loki4j (로그 수집), OpenTelemetry (분산 트레이싱) |
| **Logging** | Logback (프로파일별 전략: 콘솔/파일/Loki, 에러 로그 분리, 30일 보관) |
| **Monitoring** | Spring Boot Actuator (health, info, prometheus, metrics) |

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
│  └───────────────────────────────────┘  │ AuthFacade  (인증 오케스트레이션)│ │
│                                         │ SwaggerConfig, Rest<T>         │ │
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
│  ┌─ blog-service ────────────┐  ┌─ user-service ───────┐  ┌─ content-service ──┐ │
│  │ PostService         (CRUD) │  │ AuthService  (인증)   │  │ ContentService     │ │
│  │ CategoryService            │  │ UserService  (조회)   │  │  (MongoDB 콘텐츠)  │ │
│  │ PostTagService             │  │ CustomUserDetails     │  │ MongoConfig        │ │
│  │ PostsQueryRepository(jOOQ) │  │ UserMapper (MapStruct)│  └────────────────────┘ │
│  │ PostMapper, CategoryMapper │  │ SecurityConfig        │                         │
│  └────────────────────────────┘  └──────────────────────┘                          │
│                                                                                    │
│  ┌─ photo-archive-service ──────────────────────────────────────────────────────┐ │
│  │ PhotoArchiveFacade (@Facade) │ GoogleDriveService │ PhotoService             │ │
│  │ PhotosQueryRepository (jOOQ) │ PhotoMapper, PhotoCategoryMapper             │ │
│  └──────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                    │
│  ┌─ shared ─────────────────────────────────────────────────────────────────────┐ │
│  │ AccessTokenHandler (JWT)  │ RsaKeyHolder (RSA)    │ GoogleDriveConfig/Helper │ │
│  │ MarkdownConverter (MD→HTML) │ AsyncSupporter │ JooqConfig │ PageResponse     │ │
│  │ VirtualThreadConfig │ @Facade │ 공통 예외                                     │ │
│  └───────────────────────────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────────────────-┘
               │
┌──────────────┼────────────────────────────────────────────────────────────-┐
│              ▼              Data Access Layer                               │
│  ┌────────────────────┐  ┌──────────────────────┐  ┌──────────────────────┐ │
│  │PostsRepository     │  │UserRepository        │  │ContentDocumentRepo   │ │
│  │PostTagsRepository  │  │                      │  │ (MongoRepository)    │ │
│  │CategoriesRepository│  │                      │  │                      │ │
│  └────────┬───────────┘  └────────┬─────────────┘  └────────┬─────────────┘ │
└───────────┼───────────────────────┼──────────────────────────┼──────────────┘
            │                       │                          │
┌───────────┼───────────────────────┼──────────────────────────┼──────────────┐
│           ▼                       ▼                          ▼              │
│                             Database Layer                                   │
│  ┌──────────────────────────────────────┐  ┌─────────────────────────────┐  │
│  │         H2 (개발) / MySQL (운영)     │  │         MongoDB             │  │
│  │  users, posts, categories, posts_tags│  │  content-documents          │  │
│  └──────────────────────────────────────┘  └─────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                        Template Layer (Thymeleaf)                            │
│  ┌──────────────┐  ┌─────────────────┐  ┌──────────────────────────────┐   │
│  │layout/       │  │fragments/       │  │ Pages                        │   │
│  │ default.html │  │ navigation.html │  │ index, profile, post,        │   │
│  │ (base layout │◄─┤ footer.html     │  │ post-list, my-car, photos,   │   │
│  │  + 로그인 모달)│  └─────────────────┘  │ sre, access-denied           │   │
│  └──────────────┘                        │ (layout:decorate 적용)       │   │
│                                          ├──────────────────────────────┤   │
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
│   ├── src/main/kotlin/
│   │   └── com/walter/lifelog/
│   │       ├── LifelogApplication.kt  #   Spring Boot 메인 클래스
│   │       └── config/
│   │           ├── FilterConfig.kt    #   전역 Security 필터 (인증 경로 보호, 로그아웃)
│   │           └── JasyptConfig.kt    #   Jasypt DB 접속 정보 암호화 설정
│   └── src/main/resources/
│       ├── application.yml            #   기본(H2+MongoDB) 프로필 설정
│       ├── application-live.yml       #   운영(MySQL+MongoDB) 프로필 설정
│       └── logback-spring.xml         #   프로파일별 로깅 (콘솔/파일/Loki)
│
├── web/                               # [Web Presentation Module]
│   └── src/main/java/
│       └── com/walter/lifelog/web/
│           ├── controller/
│           │   ├── ContentController.java         # 콘텐츠 SSR (profile, my-car, access-denied)
│           │   ├── GoogleAuthController.java      # Google OAuth 2.0 인증 콜백
│           │   ├── PhotoArchiveController.java    # 사진 갤러리·업로드 SSR
│           │   ├── PhotoViewController.java       # Google Drive 이미지 서빙
│           │   ├── PostViewController.java        # 게시글 상세·에디터·목록 SSR
│           │   └── SreViewController.java         # SRE 대시보드 SSR
│           └── util/
│               └── RedirectUrlBuilder.java        # 리다이렉트 URL 생성 유틸
│
├── api/                               # [API Presentation Module]
│   └── src/main/kotlin/
│       └── com/walter/lifelog/api/
│           ├── config/
│           │   └── SwaggerConfig.kt       # OpenAPI/Swagger 설정
│           ├── facade/
│           │   └── AuthFacade.kt          # 인증 오케스트레이션 (@Facade)
│           └── controller/
│               ├── AuthController.kt      # 인증 REST API (로그인, RSA 공개키)
│               ├── PostController.kt      # 게시글 REST API (조회, 검색, 저장)
│               ├── CategoryController.kt  # 카테고리 REST API
│               ├── PhotoController.kt     # 사진 REST API (업로드, 카테고리 조회)
│               └── dto/
│                   ├── Rest.kt            # 공통 API 응답 래퍼
│                   └── PublicKeyResponse.kt  # RSA 공개키 응답 DTO
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
│           │   ├── PostListResponse.kt    # 게시글 목록 DTO
│           │   ├── PostSearchCondition.kt # 검색 조건 (키워드, 카테고리, 태그, 상태, 페이징)
│           │   ├── PageResponse.kt        # 페이징 응답 래퍼
│           │   ├── PostCategory.kt
│           │   └── CategoryTreeResponse.kt
│           ├── facade/
│           │   └── PostFacade.kt          # 비즈니스 오케스트레이션 (@Facade)
│           ├── mapper/
│           │   ├── PostMapper.kt          # MapStruct: Post ↔ DTO
│           │   └── CategoryMapper.kt      # MapStruct: Category → DTO
│           ├── repository/
│           │   ├── PostsRepository.kt     # JPA Repository
│           │   ├── PostsQueryRepository.kt  # jOOQ 동적 쿼리 (검색·페이징)
│           │   ├── CategoriesRepository.kt
│           │   └── PostTagsRepository.kt
│           └── service/
│               ├── PostService.kt         # 게시글 CRUD
│               ├── CategoryService.kt     # 카테고리 트리 조회
│               └── PostTagService.kt      # 태그 관리
│
├── content-service/                   # [Content Domain Module — MongoDB]
│   └── src/main/kotlin/
│       └── com/walter/lifelog/content/
│           ├── config/
│           │   └── MongoConfig.kt         # MongoDB Auditing 설정
│           ├── entity/
│           │   ├── ContentDocuments.kt    # MongoDB Document 엔티티
│           │   └── code/
│           │       └── ContentType.kt     # INTRO / PROFILE / CAR
│           ├── repository/
│           │   └── ContentDocumentRepository.kt  # MongoRepository
│           └── service/
│               └── ContentService.kt      # 타입별 콘텐츠 조회
│
├── photo-archive-service/             # [Photo Archive Domain Module]
│   └── src/main/kotlin/
│       └── com/walter/lifelog/photo/
│           ├── entity/
│           │   ├── Photo.kt               # 사진 엔티티 (EXIF, GPS, 썸네일 등)
│           │   ├── PhotoCategory.kt       # 사진 카테고리 엔티티
│           │   └── PhotoTag.kt            # 사진-태그 M:N 엔티티
│           ├── dto/
│           │   ├── UploadRequest.kt       # 업로드 요청 (메타데이터 + EXIF)
│           │   ├── UploadResponse.kt      # 업로드 결과 (Drive 경로·링크)
│           │   ├── PhotoSearchRequest.kt  # 사진 검색 요청 (카테고리, 페이징)
│           │   ├── PhotoSearchResponse.kt # 사진 검색 결과 (EXIF, 촬영자 등)
│           │   ├── PhotoCategoryResponse.kt # 카테고리 응답
│           │   ├── ExifInfo.kt            # EXIF 정보 DTO
│           │   └── ImageResource.kt       # 이미지 리소스 (InputStream + MIME)
│           ├── facade/
│           │   └── PhotoArchiveFacade.kt  # 사진 업로드·조회 오케스트레이션 (@Facade)
│           ├── mapper/
│           │   ├── PhotoMapper.kt         # MapStruct: UploadRequest → Photo
│           │   └── PhotoCategoryMapper.kt # MapStruct: PhotoCategory → DTO
│           ├── repository/
│           │   ├── PhotosRepository.kt    # JPA Repository
│           │   ├── PhotosQueryRepository.kt  # jOOQ 동적 쿼리 (검색·페이징)
│           │   ├── PhotoCategoriesRepository.kt
│           │   └── PhotoTagsRepository.kt
│           └── service/
│               ├── GoogleDriveService.kt  # Google Drive 업로드·이미지 서빙
│               └── PhotoService.kt        # 사진 검색·카테고리 조회
│
└── shared/                            # [Shared Infrastructure Module]
    ├── src/main/java/
    │   └── com/walter/lifelog/shared/
    │       ├── annotation/
    │       │   └── Facade.java            # @Facade 커스텀 어노테이션
    │       ├── config/
    │       │   ├── VirtualThreadConfig.java   # Virtual Thread TaskExecutor
    │       │   ├── GoogleDriveConfig.java     # Google Drive OAuth 2.0 설정
    │       │   ├── JooqConfig.java            # jOOQ 공통 설정 (실행 로깅, 포매팅)
    │       │   └── exception/
    │       │       └── PostNotFoundException.java
    │       ├── paging/
    │       │   └── PageResponse.java          # 공통 페이징 응답 DTO (record)
    │       └── util/
    │           ├── AccessTokenHandler.java    # JWT 토큰 생성/검증
    │           ├── RsaKeyHolder.java          # RSA 2048 키 쌍 관리·암복호화
    │           ├── MarkdownConverter.java      # Markdown → HTML 변환
    │           ├── GoogleDriveHelper.java     # Google Drive 파일 업로드·조회·썸네일 생성
    │           └── AsyncSupporter.java        # CompletableFuture 비동기 래퍼
    └── src/main/resources/
        └── credential.json            # Google Drive OAuth 2.0 인증 정보 (.gitignore)
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
└────────┬────────────┘       │     created_at      │              └─────────────────┘
         │                    │     updated_at      │
         │                    └──────────┬──────────┘
         │                               │
         │                    ┌──────────┼──────────┐
         │                    │          │          │
         │                    │          ▼          │
         │                    │     posts_tags      │
         │                    ├─────────────────────┤
         │                    │ PK  post_seq (FK)   │
         │                    │ PK  tag_seq         │
         │                    │     tag             │
         │                    │     created_at      │
         │                    └─────────────────────┘
         │
         │  ┌──────────────────────┐       ┌─────────────────────────┐
         │  │       photos         │       │   photos_categories     │
         │  ├──────────────────────┤       ├─────────────────────────┤
         └─►│ FK  user_seq         │       │ PK  category_seq       │
            │ FK  category_seq     │──────►│     category_name   UQ │
            │ PK  photo_seq        │       │     icon               │
            │     title            │       │     is_active          │
            │     caption          │       │     created_at         │
            │     image_url        │       │     updated_at         │
            │     thumbnail_url    │       └─────────────────────────┘
            │     exif_maker       │
            │     exif_model       │
            │     exif_aperture    │
            │     exif_shutter     │
            │     exif_iso         │
            │     exif_focal_length│
            │     exif_lens        │
            │     exif_flash       │
            │     gps_latitude     │
            │     gps_longitude    │
            │     shot_at          │
            │     like_count       │
            │     is_active        │
            │     created_at       │
            │     updated_at       │
            └──────────┬───────────┘
                       │
                       ▼
                 photos_tags
            ┌─────────────────────┐
            │ PK  photo_seq (FK)  │
            │ PK  tag_seq         │
            │     tag             │
            │     created_at      │
            └─────────────────────┘

                                         ┌──── MongoDB ─────────────────┐
                                         │                              │
                                         │   content-documents          │
                                         │ ┌──────────────────────────┐ │
                                         │ │ _id            (ObjectId)│ │
                                         │ │ contentType    (indexed) │ │
                                         │ │ content        (Map)     │ │
                                         │ │ createdAt      (audit)   │ │
                                         │ │ updatedAt      (audit)   │ │
                                         │ └──────────────────────────┘ │
                                         └──────────────────────────────┘
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
<summary>content-documents — MongoDB 콘텐츠 도큐먼트</summary>

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `_id` | ObjectId | MongoDB 고유 식별자 |
| `contentType` | String (indexed) | INTRO / PROFILE / CAR |
| `content` | Map (Object) | 콘텐츠 데이터 (유연한 Key-Value) |
| `createdAt` | LocalDateTime | 생성 일시 (`@CreatedDate`) |
| `updatedAt` | LocalDateTime | 수정 일시 (`@LastModifiedDate`) |

인덱스: `contentType` (Spring Data MongoDB `@Indexed`)

> JPA의 JSON 컬럼 방식에서 MongoDB Document로 전환하여 스키마리스 콘텐츠 관리를 실현했습니다.

</details>

<details>
<summary>photos — 사진 아카이브</summary>

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| `photo_seq` | BIGINT | PK, AUTO_INCREMENT | 사진 고유 식별자 |
| `user_seq` | BIGINT | FK, NOT NULL | 업로더 ID |
| `category_seq` | BIGINT | FK | 사진 카테고리 ID |
| `title` | VARCHAR(200) | NOT NULL | 사진 제목 |
| `caption` | TEXT | | 사진 설명 |
| `image_url` | VARCHAR(500) | NOT NULL | 원본 이미지 경로 (Google Drive) |
| `thumbnail_url` | VARCHAR(500) | | 썸네일 이미지 경로 |
| `like_count` | INT | NOT NULL, DEFAULT 0 | 좋아요 수 |
| `exif_maker` | VARCHAR(100) | | 카메라 제조사 |
| `exif_model` | VARCHAR(100) | | 카메라 모델 |
| `exif_aperture` | VARCHAR(20) | | 조리개 값 |
| `exif_shutter` | VARCHAR(20) | | 셔터 스피드 |
| `exif_iso` | VARCHAR(20) | | ISO 감도 |
| `exif_focal_length` | BIGINT | | 초점 거리 (mm) |
| `exif_lens` | VARCHAR(200) | | 렌즈 모델 |
| `exif_flash` | VARCHAR(3) | | 플래시 발광 여부 |
| `gps_latitude` | DECIMAL(10,7) | | GPS 위도 |
| `gps_longitude` | DECIMAL(11,7) | | GPS 경도 |
| `shot_at` | DATETIME | | 촬영 일시 |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT true | Soft delete 플래그 |
| `created_at` | DATETIME | NOT NULL | 생성 일시 |
| `updated_at` | DATETIME | NOT NULL | 수정 일시 |

인덱스: `idx_user_seq(user_seq)`, `idx_category_seq(category_seq)`, `idx_created_at(created_at)`

</details>

<details>
<summary>photos_categories — 사진 카테고리</summary>

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| `category_seq` | BIGINT | PK, AUTO_INCREMENT | 카테고리 고유 식별자 |
| `category_name` | VARCHAR(100) | UNIQUE, NOT NULL | 카테고리명 |
| `icon` | VARCHAR(255) | | 카테고리 아이콘 (이모지) |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT true | Soft delete 플래그 |
| `created_at` | DATETIME | NOT NULL | 생성 일시 |
| `updated_at` | DATETIME | NOT NULL | 수정 일시 |

인덱스: `idx_name(category_name)`

</details>

<details>
<summary>photos_tags — 사진-태그 M:N</summary>

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| `photo_seq` | BIGINT | PK, FK | 사진 ID |
| `tag_seq` | INT | PK | 태그 ID |
| `tag` | VARCHAR(100) | NOT NULL | 태그명 |
| `created_at` | DATETIME | NOT NULL | 생성 일시 |

인덱스: `idx_tag(tag)`

</details>

### 설계 특징

- **Polyglot Persistence**: RDB(H2/MySQL)와 MongoDB를 함께 사용. 정형 데이터는 JPA, 비정형 콘텐츠는 MongoDB Document로 저장
- **Soft Delete**: `is_active` 플래그로 논리적 삭제 구현
- **Slug 지원**: SEO 친화적 URL (`/post/my-first-blog-post`)
- **타임스탬프 자동화**: Hibernate `@CreationTimestamp`/`@UpdateTimestamp`, MongoDB `@CreatedDate`/`@LastModifiedDate`
- **계층형 카테고리**: Self-referencing FK로 트리 구조 지원
- **복합키**: `posts_tags` 테이블에서 `@IdClass`를 활용한 M:N 관계 매핑
- **Markdown 원본 보존**: `markdown_content`에 원본 저장, `content`에 HTML 변환본 저장
- **이중 인증**: 세션 기반 인증과 JWT Bearer Token 인증을 동시 지원
- **RSA 비밀번호 암호화**: 클라이언트에서 RSA 공개키로 비밀번호 암호화 → 서버에서 개인키로 복호화 (RSA-OAEP, SHA-256)
- **JWT 서명 안전성**: 짧은 시크릿 키도 SHA-256 해싱으로 256-bit HMAC 키를 보장
- **jOOQ 동적 쿼리**: 게시글 목록 검색에서 조건부 WHERE 절, JOIN, 서브쿼리를 타입 안전하게 조합
- **Google Drive 외부 스토리지**: 이미지 파일을 Google Drive에 저장하고 서버를 통해 프록시 서빙 (캐시 적용)
- **자동 썸네일 생성**: 이미지 업로드 시 600px 리사이징 썸네일을 자동 생성하여 Drive의 thumb 하위 폴더에 업로드
- **클라이언트 EXIF 추출**: 브라우저에서 exifr 라이브러리로 카메라·GPS·촬영 정보를 추출하여 서버 전송
- **DB 접속 정보 암호화**: Jasypt `ENC(...)` 방식으로 application-live.yml 내 민감 정보를 암호화
- **프로파일별 로깅**: default/dev는 콘솔만, live는 콘솔+파일(30일 rotate)+에러 파일+Loki 연동

## 시작하기

### 요구 사항
- JDK 21 이상
- (선택) MongoDB — 콘텐츠 관리용. 기본 프로필에서는 임베디드 MongoDB 사용
- (선택) Prometheus + Loki — 운영 환경 Observability

### Google Drive API 설정 (사진 아카이브 기능)

사진 업로드·서빙 기능을 사용하려면 Google Drive API 인증 파일이 필요합니다.

1. [Google Cloud Console](https://console.cloud.google.com/)에서 프로젝트를 생성합니다.
2. **API 및 서비스 > 라이브러리**에서 **Google Drive API**를 활성화합니다.
3. **API 및 서비스 > 사용자 인증 정보**에서 **OAuth 2.0 클라이언트 ID**를 생성합니다.
   - 애플리케이션 유형: 웹 애플리케이션
   - 승인된 리디렉션 URI: `http://localhost:8080/google-auth/callback` (개발 환경)
4. 생성된 OAuth 클라이언트의 JSON 파일을 다운로드합니다.
5. 다운로드한 파일의 이름을 `credential.json`으로 변경하고, 아래 경로에 배치합니다.

```
shared/src/main/resources/credential.json
```

> 이 파일은 `.gitignore`에 등록되어 있어 Git에 커밋되지 않습니다.
> 최초 실행 시 `/google-auth` 엔드포인트를 통해 OAuth 2.0 인증을 완료하면,
> 프로젝트 루트의 `tokens/` 디렉토리에 액세스 토큰이 저장됩니다.

### 프로젝트 빌드

```bash
./gradlew build
```

### 애플리케이션 실행

```bash
# 개발 환경 (H2 인메모리 DB + 임베디드 MongoDB)
./gradlew :app:bootRun

# dev 환경 (MySQL + MongoDB, Aiven Cloud)
./gradlew :app:bootRun --args='--spring.profiles.active=dev'

# 운영 환경 (MySQL + MongoDB)
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
./gradlew :photo-archive-service:test
```

## API 문서 및 모니터링

| 용도 | URL |
|------|-----|
| **Swagger UI** | http://localhost:8080/swagger-ui.html |
| **OpenAPI JSON** | http://localhost:8080/v3/api-docs |
| **Health Check** | http://localhost:8080/actuator/health |
| **Prometheus Metrics** | http://localhost:8080/actuator/prometheus |
| **H2 Console** (개발) | http://localhost:8080/h2-console |
| **SRE 대시보드** | http://localhost:8080/sre |

## 페이지 및 API 엔드포인트

### SSR 페이지 (web 모듈)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/`, `/index` | 메인 홈페이지 (MongoDB 콘텐츠 + 동적 카테고리) |
| GET | `/post-list/{page}` | 게시글 목록 (키워드·카테고리·태그 필터, 페이징) |
| GET | `/post/{inquiryStr}` | 게시글 상세 (ID 또는 Slug) |
| GET | `/post/editor` | 게시글 에디터 (신규 작성, **인증 필요**) |
| GET | `/post/editor/{postSeq}` | 게시글 에디터 (수정, **인증 필요**) |
| GET | `/profile` | 자기소개 페이지 |
| GET | `/my-car` | 애차 소개 페이지 |
| GET | `/photos` | 사진 갤러리 |
| GET | `/photos/upload` | 사진 업로드 (EXIF 추출, **인증 필요**) |
| GET | `/photo/**` | Google Drive 이미지 서빙 (캐시 적용) |
| GET | `/sre` | SRE 대시보드 |
| GET | `/access-denied` | 비인가 접근 안내 페이지 |
| GET | `/google-auth` | Google OAuth 2.0 인증 (**인증 필요**) |
| GET | `/google-auth/callback` | Google OAuth 콜백 |
| GET | `/google-auth/status` | Google 인증 상태 확인 |

### REST API (api 모듈)

| Method | Endpoint                  | 설명 |
|--------|---------------------------|------|
| POST | `/api/auth/login`         | 관리자 로그인 (RSA 복호화 → 세션 생성 + Access Token 발급) |
| GET | `/api/auth/public-key`    | RSA 공개키 조회 (비밀번호 암호화용) |
| GET | `/api/auth/status`        | 로그인 상태 확인 |
| POST | `/api/auth/logout`        | 로그아웃 (세션 무효화, JSESSIONID 삭제) |
| GET | `/api/post/{inquiryStr}`  | 게시글 조회 (ID 또는 Slug) |
| POST | `/api/post/search`        | 게시글 검색 (키워드·카테고리·태그·상태 필터, 페이징) |
| POST | `/api/post/save`          | 게시글 저장 (Bearer Token 또는 세션 인증) |
| GET | `/api/post/category/tree` | 카테고리 트리 조회 (최대 3 depth) |
| POST | `/api/photo/upload`       | 사진 업로드 (multipart/form-data, Google Drive 저장 + 썸네일 자동 생성) |
| GET | `/api/photo/categories`   | 사진 카테고리 목록 조회 |

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
| 2026-03-15 | Observability(Prometheus 메트릭·Loki 로그·OpenTelemetry 트레이싱), SRE 대시보드, Logback 프로파일별 로깅, Jasypt DB 접속 정보 암호화 |
| 2026-03-09 | photo-archive-service 모듈 구현: Google Drive 연동 이미지 업로드·서빙, 600px 썸네일 자동 생성, 클라이언트 EXIF 추출(exifr)·서버 메타데이터 저장, 사진 카테고리 관리, Photo/PhotoCategory/PhotoTag 엔티티, MapStruct Mapper, Google OAuth 2.0 인증 컨트롤러, multipart/form-data 업로드 API |
| 2026-03-08 | Polyglot Persistence 전환(content-service: JPA→MongoDB), jOOQ 도입(blog-service: 게시글 동적 검색·페이징), RSA 비밀번호 암호화(공개키 발급 API), AuthFacade 추가, 게시글 목록 SSR 페이지, 인덱스 동적 카테고리 |
| 2026-03-06 | 서비스 모듈 간 의존성 제거: 도메인 모듈은 `shared`에만 의존하도록 개선, `web`/`api`가 서비스를 조합하는 구조로 변경, 모듈 의존성 다이어그램·역할 설명 갱신 |
| 2026-03-05 | Modular Monolith Architecture 전환 반영: 8개 Gradle 서브모듈 구조 문서화, 모듈별 역할·의존성 다이어그램 추가, 아키텍처 채택 사유 기술, 미사용 의존성(Spring Cloud, WebFlux, Reactor) 정리, 기술 스택 갱신 |
| 2026-03-02 | JWT Access Token 인증 추가(JJWT), Facade 패턴 적용(`PostFacade`), Service 계층 분리(`CategoryService`, `PostTagService`, `UserService`), Bearer Token/세션 이중 인증, `LoginResponse`에 `accessToken`·`expire` 필드 추가, `AccessTokenHandlerTest` 추가 |
| 2026-03-02 | Spring Security 인증 추가, Post 저장 API, Markdown 원본 보존, Thymeleaf Layout Dialect 적용, 에디터 JS 분리, 로그인 모달, 공통 응답 형식(`Rest<T>`), Java→Kotlin 전환(DTO/Repository/Service), 테스트 코드 추가 |
| 2026-02-28 | Thymeleaf 전환 반영, PostTag 테이블 추가, Content 엔티티 Kotlin 전환, DB 드라이버 MySQL 변경, Commonmark 추가 |
| 2026-02-24 | Content 도메인(JSON 엔티티, ContentType) 추가, SSR 페이지 엔드포인트 정리, 아키텍처 다이어그램 갱신 |
| 2026-02-19 | 포트폴리오용 리팩터링, DB 스키마 상세 문서화, 아키텍처 다이어그램 추가 |
| 2026-02-03 | 초기 README 작성 |
