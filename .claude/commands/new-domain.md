# /new-domain 커맨드
# 사용법: /new-domain {DomainName}
# 예시: /new-domain Comment

$ARGUMENTS 도메인을 위한 모듈 파일 세트를 생성한다.

다음 파일들을 생성. 패키지명은 기존 모듈 구조를 참고할 것.

1. **Entity** (`{domain}-service/src/main/kotlin/.../entity/{DomainName}.kt`)
    - `data class` 사용
    - ID: `{domainName}Seq`, `GenerationType.IDENTITY`
    - `@CreationTimestamp`, `@UpdateTimestamp` 포함
    - `isActive: Boolean = true` 논리 삭제 필드 포함

2. **Repository** (`...repository/{DomainName}sRepository.kt`)
    - Spring Data JPA `JpaRepository` 상속

3. **QueryRepository** (`...repository/{DomainName}sQueryRepository.kt`)
    - jOOQ DSLContext 기반
    - companion object에 테이블/필드 상수 정의

4. **DTO** (`...dto/{DomainName}Request.kt`, `{DomainName}Response.kt`)
    - `data class` 사용

5. **Mapper** (`...mapper/{DomainName}Mapper.kt`)
    - MapStruct `@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)`

6. **Service** (`...service/{DomainName}Service.kt`)
    - `@Service`
    - 기본 CRUD 메서드 포함

7. **Facade** (`...facade/{DomainName}Facade.kt`)
    - `@Facade`
    - Service 조합 로직

생성 후 관련 테스트 파일도 함께 생성 (MockK + AssertJ, given/when/then 구조).