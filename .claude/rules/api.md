# API Controller Rules
# paths: **/api/controller/**/*.kt

## 기본 구조
```kotlin
@Tag(name = "도메인명", description = "도메인 설명 API")
@RequestMapping("/api/{domain}")
@RestController
class {Domain}Controller(
    private val facade: {Domain}Facade,
) {
    @Operation(summary = "한국어 요약", description = "한국어 상세 설명")
    @GetMapping("/{id}")
    fun get(@Parameter(description = "설명", required = true)
            @PathVariable id: Long): Rest<SomeResponse> {
        return Rest.ok(facade.get(id))
    }
}
```

## 규칙
- 응답은 반드시 `Rest<T>` 래퍼 사용 (`Rest.ok(data)`). 단, 인증을 처리하는 `api` 모듈의 `AuthController`는 `Rest` 래퍼 없이 응답 DTO를 직접 반환 (JWT 토큰 발급 등)
- Swagger 어노테이션 한국어로 작성 (`@Tag`, `@Operation`, `@Parameter`)
- 관리자 기능: `@AdminRequired` 어노테이션 + `@SecurityRequirement(name = "Authorization")`
- 관리자 API에서 userSeq 추출: `request.getAttribute("userSeq") as Long`
- URL 경로: kebab-case (예: `/api/post-list`, `/api/photo-archive`)
