# jOOQ ORM QueryRepository Rules
# paths: **/*QueryRepository.kt

## 기본 구조
```kotlin
@Repository
class {Domain}QueryRepository(
    private val dsl: DSLContext,
    private val virtualThreadExecutor: TaskExecutor,
) {
    companion object {
        private val TABLE = DSL.table("table_name")
        private val FIELD_NAME = DSL.field("table.column_name", Type::class.java)
    }
}
```

## 비동기 병렬 쿼리 패턴
count와 data를 병렬로 조회:
```kotlin
val totalCountFuture = asyncSupply(virtualThreadExecutor) {
    dsl.selectCount().from(TABLE).where(conditions).fetchOne(0, Long::class.java) ?: 0L
}
val recordsFuture = asyncSupply(virtualThreadExecutor) {
    dsl.select(...).from(TABLE).where(conditions).fetch()
}
val records = recordsFuture.get()
val totalCount = totalCountFuture.get()
```

## 규칙
- Code generation 없이 `DSL.table()`, `DSL.field()` string 방식 사용
- 동적 조건은 `mutableListOf<Condition>()` + `conditions.add(...)` 패턴
- JOIN은 `leftJoin(...).on(...)` 명시
- 페이징: `.limit(size).offset((page - 1) * size)`
- 정렬: nullable 날짜는 `.nullsLast()` 처리
