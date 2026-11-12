# ServiceLoader 프로바이더 생성자 등록

`java.util.ServiceLoader` 는 `META-INF/services/` 에 적힌 **클래스명 문자열**을 읽어
무인자 생성자로 인스턴스를 만듭니다. GraalVM 의 `reachability-metadata.json` 포맷에서
`{"type": X}` 만 있으면 `Class.forName` 은 되지만 리플렉션 인스턴스화는 허용되지
않아, 프로바이더를 찾긴 해도 만들지 못하고 다음 형태로 실패합니다.

```
java.util.ServiceConfigurationError: <서비스 인터페이스>:
  Provider <구현 클래스> could not be instantiated
Caused by: org.graalvm.nativeimage.MissingReflectionRegistrationError:
  Cannot reflectively invoke constructor 'public <구현 클래스>()'
```

tracing agent 는 이 생성자 호출을 기록해 주지 않습니다. 그래서 다음 교집합을 구해
생성자만 따로 등록합니다.

- 클래스패스 전체의 `META-INF/services` 프로바이더: 70개
- 그중 agent 실행에서 **실제로 로드된 것**: 33개
- 그중 생성자가 아직 등록되지 않은 것: **29개** ← 이 파일

로드되지 않은 프로바이더까지 등록하면 쓰이지도 않는 클래스가 image heap 을
키우므로, 실제 로드된 것으로 범위를 좁혔습니다.

## 특히 중요한 항목

`tools.jackson.module.kotlin.KotlinModule` 은 `tools.jackson.databind.JacksonModule`
프로바이더로 선언되어 ServiceLoader 로만 발견됩니다. 이 항목이 빠지면 네이티브에서
Kotlin 모듈이 등록되지 않아 **Kotlin DTO 의 기본값이 무시되고, 생략된 Boolean 이
null 로 들어와** 요청 바인딩이 깨집니다. JVM 에서 멀쩡해도 네이티브에서만 터지는
자리입니다.

## 재생성

`lifelog-runtime/reachability-metadata.json` 을 새로 수집한 뒤, 그 안의 타입 목록과
boot jar 안 모든 `META-INF/services/*` 의 교집합에서 생성자 미등록 항목을 뽑아
`{"type": T, "methods": [{"name": "<init>", "parameterTypes": []}]}` 형태로 적습니다.

프로바이더 목록을 모을 때는 중첩 jar(`BOOT-INF/lib/*.jar`)를 전부 열어야 합니다.
boot jar 최상위에는 라이브러리의 `META-INF/services` 가 보이지 않습니다.
