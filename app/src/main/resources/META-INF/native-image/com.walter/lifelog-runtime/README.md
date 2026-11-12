# 런타임 리처빌리티 메타데이터

Spring AOT 가 생성하는 힌트로는 부족한 부분을 GraalVM tracing agent 로 보충한 것입니다.
`reflection` 4,734 항목 + `resources` 1,153 항목.

설정 파일에 **클래스명 문자열**로 적는 것들이 대표적입니다. 예를 들어
`logback-spring.xml` 의 LOKI appender 는

```xml
<http class="com.walter.lifelog.config.ResilientHttpSender">
    <probeTimeoutMs>2000</probeTimeoutMs>
</http>
```

처럼 쓰는데, logback 은 이 클래스를 `Class.forName` 으로 만들고 setter 를 리플렉션으로
호출합니다. 정적 분석으로는 닿지 않으므로 생성자와 setter 를 모두 등록해야 합니다
(setter 하나가 빠지면 그 태그에서 기동이 실패합니다).

주요 출처: spring 2,211, jOOQ 278, 애플리케이션 자체 클래스 226, micrometer 141,
springdoc 131, thymeleaf 124, jackson 225(2.x 118 · 3.x 107), swagger 104,
mongodb 83, opentelemetry 73, lettuce 71, logback 27, google 25, jjwt 23, loki4j 8.

## 재수집 방법

JVM 으로 앱을 띄우고 주요 경로를 밟은 뒤 정상 종료시킵니다.
agent 는 JVM 종료 시점에 파일을 씁니다.

```bash
docker run -d --name agent --network <net> -p 19081:8080 \
  -v "$PWD/app/build/libs/app-0.0.1-SNAPSHOT.jar:/app/boot.jar:ro" \
  -v "$PWD/out:/out" -w /app \
  -v "$PWD/config/credential.json:/app/config/credential.json:ro" \
  <MySQL·MongoDB·Redis·OpenAI 환경변수> \
  --entrypoint java ghcr.io/graalvm/native-image-community:25 \
    -agentlib:native-image-agent=config-output-dir=/out \
    -Djava.awt.headless=true -jar /app/boot.jar

# 아래 "밟아야 하는 것" 을 호출
docker stop -t 60 agent      # SIGTERM 으로 정상 종료 — kill 하면 파일이 안 생깁니다
```

### 밟아야 하는 것

밟지 않은 경로는 수집되지 않습니다. 빠지기 쉬운 네 가지가 있습니다.

1. **데이터가 있어야 밟히는 경로.** 글·사진이 없는 DB 로 수집하면 `/post/{slug}`,
   `/photos` 의 렌더링이 빈 화면으로 끝나 `PostView`·`PostCategory` 와 Thymeleaf
   처리기 상당수가 누락됩니다. 카테고리·발행 글·사진을 미리 넣어 두십시오.
2. **같은 경로를 두 번.** 캐시된 응답의 생성자는 **캐시에서 읽을 때**
   역직렬화로 불립니다. 한 번만 호출하면 쓰기 경로만 잡혀 `PostSimpleInfo`,
   `PostResponse` 등의 생성자가 통째로 빠집니다.
3. **`credential.json`.** 없으면 `GoogleDriveConfig` 가 기동 중 예외를 던지는데,
   JVM 에서는 넘어가도 네이티브는 AOT 로 빈을 즉시 만들어 기동이 실패합니다.
   실제 파일은 **web 타입**이므로 더미를 쓸 때도 `"web"` 키로 맞추십시오
   (`"installed"` 로 두면 `setInstalled` 가 등록되어 있지 않아 다른 데서 깨집니다).
4. **관리자 경로.** 로그인 후 `/api/static-page/**`, `/api/post/save`,
   `/api/content` 등을 호출해야 합니다.

썸네일(ImageIO) 경로는 Google Drive 인증이 필요해 여기서 밟을 수 없으므로
`../lifelog-awt/` 에 따로 수집해 두었습니다.

### 기존 파일과 합치기

새로 수집한 결과로 **그냥 덮어쓰지 마십시오.** 이번 수집에서 안 밟힌 경로가
기존 파일에는 들어 있을 수 있습니다. 다음 순서로 합칩니다.

1. boot jar 의 중첩 jar(`BOOT-INF/lib/*.jar`)까지 전부 열어 클래스패스에 실제로
   존재하는 클래스 목록을 만듭니다. `unzip -l` 로 보이는 최상위 목록만 쓰면
   라이브러리 클래스가 전부 "없음" 으로 잘못 판정됩니다.
2. 기존 파일에만 있는 타입 중 **클래스패스에 남아있는 것**만 되살립니다.
   (의존성이 제거되어 사라진 클래스는 버립니다.)
3. 양쪽에 다 있는 타입은 **메서드·필드 단위로 합집합**을 취합니다.
   타입 단위로만 비교하면 위 2번 같은 생성자 누락을 놓칩니다.

### 검증

빌드 성공은 검증이 아닙니다. 누락은 거의 전부 **런타임에** 터집니다.
네이티브 이미지를 빌드한 뒤 실제로 띄워서 위 경로를 두 번씩 밟고,
로그에 `MissingReflectionRegistrationError` 가 없는지 확인하십시오.
