# Facade Rules
# paths: **/facade/**/*.kt

## 기본 구조
```kotlin
@Facade
class {Domain}Facade(
    private val serviceA: ServiceA,
    private val serviceB: ServiceB,
    private val virtualThreadExecutor: TaskExecutor,
    private val transactionTemplate: TransactionTemplate,
) {
    @Transactional(readOnly = true)
    fun getSomething(id: Long): SomeResponse {
        // 여러 Service 조합
    }
}
```

## 규칙
- `@Facade` 커스텀 어노테이션 사용 (`@Component` 상위 어노테이션 포함)
- 쓰기 작업: `@Transactional` 또는 `transactionTemplate.execute { ... }` 사용
- 읽기 작업: `@Transactional(readOnly = true)`
- 비동기 병렬 호출: `asyncSupply(virtualThreadExecutor) { service.method() }`
- Facade는 Repository 직접 접근 금지
- 단일 Service만 사용하는 경우 Facade 생성 불필요 — Controller에서 직접 Service 호출
- 복잡한 트랜잭션 관리가 필요한 경우에만 Facade 생성, 단순 조합은 Service에서 처리
- 비즈니스 로직의 흐름을 순차적으로 정의하고, 파악하기 쉽게 Method 내용을 구성
