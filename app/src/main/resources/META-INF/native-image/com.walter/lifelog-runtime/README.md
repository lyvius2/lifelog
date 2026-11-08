# 런타임 리처빌리티 메타데이터

Spring AOT 가 생성하는 힌트로는 부족한 부분을 GraalVM tracing agent 로 보충한 것입니다.
`reflection` 5,217 항목 + `resources` 1,027 항목.

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

주요 출처: jOOQ 278, thymeleaf 219, 애플리케이션 자체 클래스 216, springdoc 131,
logback 27, loki4j 8.

## 재수집 방법

JVM 으로 앱을 띄우고 주요 경로를 한 번씩 밟은 뒤 정상 종료시킵니다.
agent 는 JVM 종료 시점에 파일을 씁니다.

```bash
docker run -d --name agent --network <net> -p 19081:8080 \
  -v "$PWD/app/build/libs/app-0.0.1-SNAPSHOT.jar:/app/boot.jar:ro" \
  -v "$PWD/out:/out" -w /app \
  <MySQL·MongoDB·Redis·OpenAI 환경변수> \
  --entrypoint java ghcr.io/graalvm/native-image-community:25 \
    -agentlib:native-image-agent=config-output-dir=/out \
    -Djava.awt.headless=true -jar /app/boot.jar

# /, /post-list/1, /photo-archive, /architecture, /ai-ops,
# /actuator/{health,prometheus,info}, /swagger-ui/index.html, /v3/api-docs 호출
docker stop -t 30 agent      # SIGTERM 으로 정상 종료 — kill 하면 파일이 안 생깁니다
```

밟지 않은 경로는 수집되지 않습니다. 새 화면이나 API 를 추가하면 다시 수집하십시오.
썸네일(ImageIO) 경로는 Google Drive 인증이 필요해 여기서 밟을 수 없으므로
`../lifelog-awt/` 에 따로 수집해 두었습니다.
