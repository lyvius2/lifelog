# DB 복구 / 이전 가이드

기존 DB(Aiven MySQL · MongoDB Atlas)를 더 쓸 수 없게 되어
새 인스턴스로 옮길 때 쓰는 파일들입니다.

> PostgreSQL 은 더 이상 복구 대상이 아닙니다. `posts_log` 를 적재하던 worker 모듈을
> 제거했기 때문입니다. 다만 구 DB 에 남은 `posts_log` 는 되돌릴 수 없는 과거
> 데이터이므로, `00_dump_old.sh` 는 **보관 목적으로** 여전히 덤프를 받습니다.

## 먼저 읽을 것

**이 디렉터리의 DDL 은 "구조"를 복구합니다. "내용"은 복구하지 못합니다.**

| 복구 가능 | 복구 불가 (덤프가 필요) |
|---|---|
| 테이블·인덱스·제약조건 전부 | `posts` 게시글 본문 |
| DB 계정과 권한 | `photos` 사진 메타데이터·EXIF |
| 관리자 계정 1개 (로그인 복구용) | `users` 실제 사용자들 |
| 카테고리 자리값 (예시) | `posts_log` 활동 로그 (보관 덤프만) |
| MongoDB `ARCHITECTURE`·`AI_OPS_DESIGN` 문서 | MongoDB `INTRO`·`PROFILE`·`CAR` 문서 |

게시글과 사진은 코드 어디에도 없습니다. **구 DB 에 아직 접속이 된다면 다른 작업보다
먼저 `./db/00_dump_old.sh` 를 돌려 덤프를 받으십시오.** 접속이 끊긴 뒤에는 방법이 없습니다.

MongoDB 콘텐츠 문서 2개는 저장소 루트에 남아 있던 `archi.json`, `aiops.json` 에서
살렸습니다 (`mongodb/content-documents.ndjson`).

## 파일 구성

```
db/
├── 00_dump_old.sh                  구 DB 전량 덤프 (가장 먼저)
├── dump.env.example                덤프용 접속 정보 템플릿
├── mysql/
│   ├── lifelog_init.sql            계정·DB·테이블·인덱스·초기 데이터 전부 (단일 파일)
│   └── lifelog_charset_fix.sql     기존 DB 의 문자셋을 utf8mb4 로 교정 (아래 참고)
└── mongodb/
    ├── 01_init.js                  컬렉션 + contentType 인덱스
    └── content-documents.ndjson    복구된 콘텐츠 문서 2건
```

## 실행 순서

### 0. 구 DB 덤프 (접속 가능한 경우에만)

```bash
cp db/dump.env.example db/dump.env    # 접속 정보 입력. dump.env 는 gitignore 됩니다
./db/00_dump_old.sh                   # db/dump/<타임스탬프>/ 에 저장
```

받은 덤프는 저장소 밖 다른 매체에도 복사해 두십시오.

### 1. MySQL

파일 하나로 끝납니다. **DB 를 지정하지 않고** 관리자 권한으로 실행하십시오.
계정 생성 → DATABASE 생성 → 테이블·인덱스 → 초기 데이터 순으로 한 번에 처리합니다.

```bash
mysql -h <host> -P <port> -u <admin> -p --ssl-mode=REQUIRED \
      --default-character-set=utf8mb4 < db/mysql/lifelog_init.sql
```

실행 전에 파일 안의 플레이스홀더를 바꾸십시오.

| 위치 | 플레이스홀더 |
|---|---|
| 2절 계정 생성 | `CHANGE_ME_APP_PASSWORD`, `CHANGE_ME_DDL_PASSWORD` |
| 0절 변수 | `@admin_email`, `@admin_name`, `@admin_display_name` |

끝에 검증용 SELECT 가 붙어 있어, 테이블 7개·계정 2개·관리자 1명·카테고리 6건이
출력되면 정상입니다.

**멱등합니다.** `IF NOT EXISTS` 와 `ON DUPLICATE KEY UPDATE` 를 써서 여러 번 돌려도
기존 데이터를 덮어쓰지 않습니다.

덤프를 복원할 거라면 초기 데이터 대신 덤프를 넣습니다. 이 파일을 먼저 돌려
구조를 만든 뒤:

```bash
mysql ... lifelog < db/dump/<타임스탬프>/mysql_data_only.sql
```

구조까지 덤프로 복원하려면 `mysql_full.sql` 하나만 넣고 `lifelog_init.sql` 의
3절은 건너뜁니다. 그 경우 **구 DB 의 구조가 현재 엔티티와 맞는지** 먼저 비교하십시오
(`mysql_schema_only.sql` 과 `lifelog_init.sql` 의 3절을 diff).

### 2. MongoDB

```bash
MONGO_URI="mongodb+srv://<user>:<pw>@<cluster>/lifelog"

mongosh "$MONGO_URI" --file db/mongodb/01_init.js

mongoimport --uri "$MONGO_URI" \
  --collection content-documents \
  --file db/mongodb/content-documents.ndjson
```

`INTRO`(메인) · `PROFILE` · `CAR` 문서는 백업이 없습니다. 로그인 후
`/content/editor` 에서 직접 입력하십시오. 문서가 없어도 페이지는 뜨고 내용만 빕니다
(`ContentService.getContentByType` 이 빈 Map 을 반환).

### 3. 애플리케이션 접속 정보 교체

**MySQL 은 환경변수로 넣습니다.** `application-live.yml` 을 고칠 필요가 없습니다.

```bash
MYSQL_HOST=<host>
MYSQL_PORT=3306
MYSQL_DATABASE=lifelog
MYSQL_USERNAME=lifelog_app
MYSQL_PASSWORD=<위에서 설정한 비밀번호>
MYSQL_SSL_MODE=REQUIRED
```

템플릿은 `docker/lifelog.env.example` 에 있습니다. URL 전체를 직접 지정하려면
`SPRING_DATASOURCE_URL` 로 덮어쓸 수 있습니다.

`MYSQL_SSL_MODE` 는 Connector/J 의 실제 속성명인 `sslMode` 로 들어갑니다.
예전에 쓰던 `ssl-mode` 는 **Connector/J 가 모르는 속성을 조용히 무시**하기 때문에
아무 효과가 없었고, 실제로는 기본값 `PREFERRED` 로 동작하고 있었습니다.

MongoDB 와 Redis 도 환경변수로 받습니다. `application-live.yml` 에는 비밀값을 두지
않고 `${...}` 자리만 있으므로, 접속 정보가 바뀌면 `docker/lifelog.env` 의 값만
고치면 됩니다. 암호화(Jasypt) 는 2026-11-08 에 걷어냈습니다.

| 항목 | 환경변수 | 기본값 |
|---|---|---|
| MySQL | `MYSQL_*` (또는 `SPRING_DATASOURCE_URL` 로 URL 통째로) | `HOST`·`USERNAME`·`PASSWORD` 없음 |
| MongoDB | `MONGODB_URI` | 없음 |
| Redis | `REDIS_URL`, `REDIS_SSL_ENABLED` | URL 없음 / SSL `true` |

기본값이 없는 항목은 비우면 플레이스홀더 해석 단계에서 기동이 실패합니다.
Redis 는 `live` 프로파일에서만 쓰며, 다른 프로파일은 로컬 메모리 캐시로 돕니다.

## 검증

`lifelog_init.sql` 은 실제 MySQL 8.0 컨테이너에서 다음을 확인했습니다.

- 두 번 연속 실행해도 오류 없고 데이터가 중복되지 않음 (멱등)
- 테이블 7개 전부 InnoDB / `utf8mb4_unicode_ci`, 이모지(`🏞️`)가 깨지지 않음
- `lifelog_app` 계정으로 조회수 동기화 쿼리(`UPDATE posts SET view_count`) 가능
- `lifelog_app` 계정으로 `DROP` 은 거부됨 (최소 권한 확인)
- 이 SQL 로 만든 DB 에 `JooqRepositoryWriteTest` 를 붙여 쓰기 경로 14건 통과
  (생성 키 수신, 부분 update, 멀티 insert, 집계, GPS 소수점 7자리 보존)

**`ddl-auto: validate` 에 의한 스키마 검증은 더 이상 없습니다.** JPA 를 걷어내고
jOOQ 로 전환했기 때문입니다(2026-11-08). 예전에는 엔티티와 DB 가 어긋나면 기동
자체가 실패해 즉시 알 수 있었지만, 지금은 **해당 쿼리가 실행되는 시점에** 드러납니다.
컬럼을 바꿀 때는 아래 명령으로 쓰기 경로를 직접 확인하십시오.

```bash
SPRING_DATASOURCE_URL="jdbc:mysql://127.0.0.1:3306/lifelog?sslMode=DISABLED&allowPublicKeyRetrieval=true&characterEncoding=UTF-8" \
SPRING_DATASOURCE_USERNAME=lifelog_app \
SPRING_DATASOURCE_PASSWORD=<비밀번호> \
SPRING_DATASOURCE_DRIVERCLASSNAME=com.mysql.cj.jdbc.Driver \
  ./gradlew :app:test --tests "com.walter.lifelog.JooqRepositoryWriteTest"
```

테스트는 자기가 만든 행만 지웁니다(`zz-test-` 접두사). 다만 `findShotPeriod` 가
활성 사진 전체를 집계하므로 **검증용 DB** 에 붙이십시오.
나아가 **이 SQL 로 만든 DB 에 컨테이너 이미지를 붙여 기동까지 확인**했습니다
(`/`, `/post-list/1`, `/photo-archive`, `/architecture` 전부 200).

## 알아 둘 점

### `posts.status` 는 네이티브 ENUM 입니다

원래 Hibernate 7 + `MariaDBDialect` 가 `@Enumerated(EnumType.STRING)` 을
`ENUM('ARCHIVED','DRAFT','PUBLISHED')` 로 매핑한 결과입니다. JPA 를 걷어낸 뒤로
`validate` 가 막아 주지 않으므로 `VARCHAR(20)` 으로 바꿔도 기동은 됩니다. 다만
jOOQ 가 `PostStatus.name()` 문자열을 그대로 넣고 읽으므로 **ENUM 에 없는 값을
넣으려 하면 그때 실패합니다.** 열거된 값은 `PostStatus` enum 과 일치시켜 두십시오.

### `is_active` 는 `BIT` 입니다

`BOOLEAN`/`TINYINT(1)` 이 아니라 `BIT` 입니다. 삽입할 때는 `b'1'` / `b'0'` 을 씁니다.

### GPS 컬럼은 `decimal(10,7)` / `decimal(11,7)` 입니다

예전 `Photo` 엔티티에는 `precision`/`scale` 선언이 없어 Hibernate 가
`decimal(38,2)` 를 만들었습니다. **소수점 2자리면 위경도가 약 1.1km 단위로
뭉개집니다.** 그래서 `lifelog_init.sql` 은 자릿수를 직접 선언했습니다.

JPA 를 걷어낸 뒤로는 스키마를 이 파일만 만들므로 엔티티 쪽에 할 일이 없습니다.
`JooqRepositoryWriteTest` 의 `savePhoto` 테스트가 소수점 7자리가 보존되는지
(`35.0116363` / `135.7681156`) MySQL 에 대고 확인합니다.

### `aiops.json` 의 contentType 이 enum 과 어긋나 있었습니다

파일에는 `AI_OPS` 로 적혀 있지만 `ContentType` enum 에는 `AI_OPS_DESIGN` 만
있습니다. `content-documents.ndjson` 을 만들 때 `AI_OPS_DESIGN` 으로 교정했습니다.
교정하지 않으면 `ContentService.getAllTypeInfos()` 가 `findAll()` 역직렬화에서
깨집니다.

### 조회수는 스케줄러가 뒤늦게 반영합니다

`posts.view_count` 는 조회 시점에 갱신되지 않습니다. 조회수는 Redis 에 `INCR` 로
쌓이고, `PostViewCountSyncService` 가 10 분마다 MySQL 로 옮깁니다. 복구 직후
`view_count` 가 0 으로 보이는 것은 정상이며, Redis 까지 새로 만든 경우에는 과거
조회수가 복원되지 않습니다.

이 스케줄러는 `lifelog_app` 계정으로 `posts.view_count` 를 UPDATE 합니다.
`lifelog_init.sql` 2절이 주는 권한에 이미 포함되어 있어 추가 GRANT 는 필요 없습니다.

### 한글을 저장하면 `Incorrect string value` 가 납니다

```
java.sql.SQLException: Incorrect string value: '\xED\x85\x8C\xEC\x8A\xA4...'
    for column 'title' at row 1
```

테이블이 `latin1` 로 만들어져 있습니다. `lifelog_init.sql` 은 모든 테이블에
`utf8mb4` 를 선언하지만 `CREATE TABLE IF NOT EXISTS` 라서, 문자셋 선언이 없던
시절에 만들어진 테이블이 이미 있으면 그대로 건너뜁니다. 애플리케이션 설정
(`characterEncoding=UTF-8`) 은 이미 맞으므로 바꿀 것이 없습니다.

대상인지 확인합니다. 스키마를 지정해 거르면 접속한 DB 가 앱이 쓰는 DB 와
다를 때 빈 결과가 나오므로, 전체를 훑습니다. (`TABLE_SCHEMA = DATABASE()` 는
`USE` 를 하지 않은 세션에서 `DATABASE()` 가 NULL 이라 항상 빈 결과입니다.)

```sql
SELECT TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME, CHARACTER_SET_NAME
  FROM information_schema.COLUMNS
 WHERE CHARACTER_SET_NAME IS NOT NULL
   AND CHARACTER_SET_NAME <> 'utf8mb4'
   AND TABLE_SCHEMA NOT IN ('mysql', 'information_schema', 'performance_schema', 'sys');
```

한 행이라도 나오면 교정합니다. 덤프를 먼저 받아두십시오.

빈 결과인데도 오류가 계속된다면 접속한 서버가 앱의 접속처와 다른 것입니다.
오류 자체는 목적지 컬럼이 utf8mb4 가 아닐 때만 납니다 — 접속 문자셋이 틀린
경우에는 오류 없이 값만 조용히 깨집니다(`í…ŒìŠ¤íŠ¸` 같은 모양). 앱이 실제로
어디에 붙는지 `MYSQL_HOST` · `MYSQL_DATABASE` 로 확인하십시오.

```bash
mysql -h <host> -P <port> -u <user> -p lifelog < db/mysql/lifelog_charset_fix.sql
```

`ALTER TABLE ... CONVERT TO` 는 저장된 값을 새 문자셋으로 변환해 옮기므로 기존
데이터는 보존됩니다. 다만 이미 깨진 채로 들어간 값은 깨진 상태 그대로 옮겨지니,
교정 후 눈으로 확인하고 필요하면 다시 저장하십시오.

### 비밀번호

`lifelog_init.sql` 의 비밀번호는 모두 플레이스홀더입니다.
이 저장소는 공개 라이선스이므로 실제 값을 커밋하지 마십시오.
관리자 계정의 BCrypt 해시는 평문 `ChangeMe!2026` 이며, 로그인 직후 교체하십시오.
새 해시를 만드는 방법은 파일 안 주석에 적어 두었습니다.
