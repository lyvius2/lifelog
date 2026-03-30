# /new-api 커맨드
# 사용법: /new-api {HttpMethod} {path} {설명}
# 예시: /new-api GET /api/post/{postSeq}/comments 게시글 댓글 목록 조회

$ARGUMENTS 에 해당하는 API 엔드포인트를 추가한다.

다음 순서로 구현:

1. **Controller** (`api/` 모듈 해당 Controller에 메서드 추가)
    - `Rest<T>` 래퍼 응답
    - `@Operation(summary = "...", description = "...")` 한국어 Swagger 문서
    - `@Parameter` 어노테이션으로 파라미터 설명

2. **Facade** (기존 Facade에 메서드 추가 또는 신규 생성)
    - 필요 시 `@Transactional(readOnly = true)` 또는 `transactionTemplate` 사용

3. **Service** (도메인 작은 단위 비즈니스 로직 구현)

4. **DTO** (Request/Response 필요 시 추가)

5. **테스트** (Service 단위 테스트 + Facade 단위 테스트)
    - MockK + AssertJ
    - `@DisplayName` 한국어
    - given / when / then 구조
 