# AWT / ImageIO 리처빌리티 메타데이터

`GoogleDriveHelper.resizeImage` 가 쓰는 `ImageIO` / `Graphics2D` 경로는 리플렉션으로만
닿기 때문에 native-image 의 정적 분석이 찾지 못합니다. 메타데이터가 없으면
`libawt.so` 의 `JNI_OnLoad` 가 `FindClass("java/awt/GraphicsEnvironment")` 에 실패하고
`BufferedImage` 클래스 초기화에서 죽습니다.

이 파일은 GraalVM 25 의 tracing agent 로 수집했습니다. JPEG/PNG 디코딩 → 리사이즈 →
재인코딩을 실제로 한 번 실행시켜 얻은 결과입니다.

재수집이 필요하면 (예: 썸네일 코드가 바뀌어 새 API 를 쓰게 된 경우):

```bash
docker run --rm -v "$PWD/<실행코드>:/src:ro" -v "$PWD/out:/out" \
  --entrypoint sh ghcr.io/graalvm/native-image-community:25 -c '
    javac -d /work /src/ThumbCheck.java
    java -Djava.awt.headless=true \
         -agentlib:native-image-agent=config-output-dir=/out \
         -cp /work ThumbCheck'
```

`ThumbCheck` 는 `resizeImage` 와 같은 호출 순서를 재현하는 작은 프로그램입니다.
