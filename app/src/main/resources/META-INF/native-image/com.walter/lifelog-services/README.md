# ServiceLoader 프로바이더 생성자 등록

`java.util.ServiceLoader` 는 `META-INF/services/` 에 적힌 **클래스명 문자열**을 읽어
무인자 생성자로 인스턴스를 만듭니다. GraalVM 의 새 `reachability-metadata.json`
포맷에서 `{"type": X}` 만 있으면 `Class.forName` 은 되지만 리플렉션 인스턴스화는
허용되지 않아 다음처럼 실패합니다.

```
java.util.ServiceConfigurationError: org.hibernate.bytecode.spi.BytecodeProvider:
  Provider org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl could not be instantiated
Caused by: org.graalvm.nativeimage.MissingReflectionRegistrationError:
  Cannot reflectively invoke constructor 'public ...BytecodeProviderImpl()'
```

tracing agent 는 이 생성자 호출을 기록해 주지 않았습니다. 그래서 다음 교집합을
구해 생성자만 따로 등록합니다.

- 클래스패스 전체의 `META-INF/services` 프로바이더: 77개
- 그중 agent 실행에서 **실제로 로드된 것**: 31개
- 그중 생성자가 아직 등록되지 않은 것: **27개** ← 이 파일

로드되지 않은 프로바이더까지 등록하면 쓰이지도 않는 클래스가 image heap 을
키우므로, 실제 로드된 것으로 범위를 좁혔습니다.

## 재생성

`lifelog-runtime/reachability-metadata.json` 을 새로 수집한 뒤, 그 안의 타입 목록과
boot jar 안 모든 `META-INF/services/*` 의 교집합에서 생성자 미등록 항목을 뽑아
`{"type": T, "methods": [{"name": "<init>", "parameterTypes": []}]}` 형태로 적습니다.

## 주의: hibernate.bytecode.provider=none 은 쓸 수 없습니다

네이티브에서 흔히 쓰는 처방이지만, Hibernate 의 `none` 프로바이더는
`DisallowedProxyFactory` 를 돌려주고 호출 시 예외를 던집니다. 이 프로젝트의 엔티티는
`@ManyToOne(fetch = FetchType.LAZY)` 를 쓰므로 프록시가 필요합니다.
`none` 으로 바꾸려면 빌드 시점 엔티티 enhancement 를 먼저 도입해야 합니다.
