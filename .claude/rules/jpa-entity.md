# JPA Entity Rules
# paths: **/entity/**/*.kt

## 필수 패턴
- ID 필드명: `{domain}Seq` 형식 (예: `postSeq`, `photoSeq`)
- ID 전략: `@GeneratedValue(strategy = GenerationType.IDENTITY)`
- `@Column(name = "snake_case_name")` 명시
- Enum 필드: `@Enumerated(EnumType.STRING)` 필수
- 타임스탬프: `@CreationTimestamp` (createdAt), `@UpdateTimestamp` (updatedAt)
- 클래스: `data class` 선언

## 논리 삭제
- 삭제는 반드시 `is_active = false`로 처리
- 물리 삭제 (`deleteById`, `delete`) 사용 금지

## FK 패턴 (ManyToOne)
```kotlin
@Column(name = "category_seq")
val categorySeq: Long? = null
 
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "category_seq", insertable = false, updatable = false)
val category: Category? = null
```

## Index 선언
자주 조회되는 컬럼은 `@Table(indexes = [...])` 로 Index 명시
