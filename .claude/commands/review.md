# /review 커맨드
# 사용법: /review {파일경로 또는 "현재 변경사항"}
# 예시: /review blog-service/src/main/kotlin/com/walter/lifelog/blog/service/PostService.kt

$ARGUMENTS 파일(또는 현재 변경사항)을 다음 관점에서 리뷰할 것.

## 체크리스트

### 🔴 위험 (반드시 수정)
- [ ] `thymeleaf.cache: false`가 application.yml (공통)에 있는가?
- [ ] VirtualThreadConfig에 Semaphore 없이 무제한 스레드 생성하는가?
- [ ] Entity 물리 삭제 (`delete`, `deleteById`) 사용하는가?
- [ ] live 프로파일에 `ddl-auto: create` 또는 `create-drop` 설정이 있는가?
- [ ] 비밀값(password, api-key, secret)이 암호화 없이 yml에 평문으로 있는가?

### 🟡 주의 (개선 권장)
- [ ] Service에서 다른 Service를 직접 의존하는가? (Facade 통해 조합할 것)
- [ ] jOOQ QueryRepository에서 N+1 문제가 있는가?
- [ ] `@Transactional` 누락된 쓰기 작업이 있는가?
- [ ] 테스트 코드에 `given / when / then` 구조가 빠져 있는가?
- [ ] `@DisplayName`이 한국어로 작성되어 있는가?

### 🟢 스타일 (선택적 개선)
- [ ] Kotlin scope function으로 단순화 가능한 코드가 있는가?
- [ ] companion object에 상수 정의가 빠져 있는가?
- [ ] 함수가 30줄을 초과하는가?

위 체크리스트 결과를 항목별로 정리하고, 문제가 있는 경우 수정 코드를 제안바랍니다.
