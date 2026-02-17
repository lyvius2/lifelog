# Lifelog 📝

일상을 기록하는 간단한 홈페이지 프로젝트입니다.

## 기술 스택

- **Language**: Kotlin 2.2.21
- **Framework**: Spring Boot 4.0.2
- **Template Engine**: Mustache
- **Database**: H2 (개발), MariaDB (운영)
- **Build Tool**: Gradle (Kotlin DSL)
- **API Documentation**: Springdoc OpenAPI (Swagger)
- **Monitoring**: Spring Boot Actuator, Prometheus

## 요구 사항

- JDK 21 이상

## 시작하기

### 프로젝트 빌드

```bash
./gradlew build
```

### 애플리케이션 실행

```bash
./gradlew bootRun
```

실행 후 브라우저에서 http://localhost:8080 으로 접속하세요.

## API 문서 및 모니터링

### Swagger UI
API 문서는 아래 URL에서 확인할 수 있습니다:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

### Actuator Endpoints
Spring Boot Actuator를 통한 애플리케이션 모니터링:
- **Health Check**: http://localhost:8080/actuator/health
- **Application Info**: http://localhost:8080/actuator/info
- **Prometheus Metrics**: http://localhost:8080/actuator/prometheus
- **All Metrics**: http://localhost:8080/actuator/metrics

### H2 Database Console
개발 환경에서 H2 데이터베이스 콘솔:
- **H2 Console**: http://localhost:8080/h2-console
- **JDBC URL**: `jdbc:h2:mem:testdb`
- **Username**: `sa`
- **Password**: (비워두기)

## 데이터베이스 스키마

### Users (사용자)
- 사용자 정보 관리
- 이메일, 이름, 프로필 정보

### Categories (카테고리)
- 게시글 분류를 위한 카테고리
- 계층 구조 지원 (parent_category_id)

### Posts (게시글)
- 블로그 게시글 관리
- 상태: DRAFT, PUBLISHED, ARCHIVED
- 전문 검색(Fulltext) 지원

## 프로젝트 구조

```
src/
├── main/
│   ├── kotlin/
│   │   └── com/walter/lifelog/
│   │       ├── LifelogApplication.kt
│   │       ├── config/          # 설정 파일 (Swagger 등)
│   │       ├── controller/      # REST API 컨트롤러
│   │       ├── entity/          # JPA 엔티티
│   │       ├── repository/      # 데이터 접근 계층
│   │       └── service/         # 비즈니스 로직
│   └── resources/
│       ├── application.yml      # 애플리케이션 설정
│       ├── application-live.yml # 운영 환경 설정
│       ├── static/              # 정적 파일 (CSS, JS, 이미지)
│       └── templates/           # Mustache 템플릿
└── test/
    └── kotlin/
        └── com/walter/lifelog/
            └── LifelogApplicationTests.kt
```

## 프로필 설정

### 개발 환경 (기본)
```bash
./gradlew bootRun
```

### 운영 환경
```bash
./gradlew bootRun --args='--spring.profiles.active=live'
```

## 라이선스

MIT License
