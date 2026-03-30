# Test Rules
# paths: **/*Test.kt, **/*Test.java

## 필수 구조
```kotlin
@DisplayName("ClassName 테스트")
class SomeServiceTest {
 
    private val dependency: Dependency = mockk()
    private val sut = SomeService(dependency = dependency)
 
    @Test
    @DisplayName("메서드명 - 상황 설명 시 기대 동작을 설명한다")
    fun methodName_situation_expectedBehavior() {
        // given
 
        // when
 
        // then
    }
}
```

## Mock 규칙
- Kotlin 파일: MockK (`mockk()`, `every`, `verify`, `just Runs`)
- Java 파일: Mockito
- Static mock: `mockkStatic(Class::class)` + `@AfterEach unmockkStatic()`
- VirtualThread가 필요한 테스트: `TaskExecutor { Executors.newVirtualThreadPerTaskExecutor().execute(it) }`

## Assertion
- AssertJ: `assertThat(result).isEqualTo(expected)`
- 호출 검증: `verify(exactly = 1) { mock.method(arg) }`

## fixture 헬퍼
테스트용 객체는 `private fun create{EntityName}(...)` 메서드로 분리
