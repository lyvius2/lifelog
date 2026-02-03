# Lifelog 📝

일상을 기록하는 간단한 홈페이지 프로젝트입니다.

## 기술 스택

- **Language**: Kotlin 2.2.21
- **Framework**: Spring Boot 4.0.2
- **Template Engine**: Mustache
- **Database**: H2 (개발), MariaDB (운영)
- **Build Tool**: Gradle (Kotlin DSL)

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

## 프로젝트 구조

```
src/
├── main/
│   ├── kotlin/
│   │   └── com/walter/lifelog/
│   │       └── LifelogApplication.kt
│   └── resources/
│       ├── application.yml
│       ├── static/          # 정적 파일 (CSS, JS, 이미지)
│       └── templates/       # Mustache 템플릿
└── test/
    └── kotlin/
        └── com/walter/lifelog/
            └── LifelogApplicationTests.kt
```

## 라이선스

MIT License
