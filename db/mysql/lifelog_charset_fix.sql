-- 기존 DB 의 문자셋을 utf8mb4 로 교정합니다.
--
-- 증상: 한글이 든 게시글을 저장하면 MySQL 이 아래 오류를 냅니다.
--   Incorrect string value: '\xED\x85\x8C\xEC\x8A\xA4...' for column 'title' at row 1
--
-- 원인: 테이블이 latin1 로 만들어져 있습니다. lifelog_init.sql 은 모든 테이블에
-- utf8mb4 를 선언하지만 CREATE TABLE IF NOT EXISTS 라서, 문자셋 선언이 없던
-- 시절에 만들어진 테이블이 이미 있으면 그대로 건너뜁니다.
--
-- 적용 전에 반드시 덤프를 받아두십시오 (db/README.md 참고).
--   mysql -h <host> -P <port> -u <user> -p lifelog < db/mysql/lifelog_charset_fix.sql

-- ── 1. 적용 전 확인 ────────────────────────────────────────────────
-- utf8mb4 가 아닌 컬럼이 나오면 교정 대상입니다.
SELECT TABLE_NAME, COLUMN_NAME, CHARACTER_SET_NAME, COLLATION_NAME
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE()
   AND CHARACTER_SET_NAME IS NOT NULL
   AND CHARACTER_SET_NAME <> 'utf8mb4'
 ORDER BY TABLE_NAME, ORDINAL_POSITION;

-- ── 2. 교정 ───────────────────────────────────────────────────────
-- 앞으로 만들어질 테이블의 기본값
ALTER DATABASE lifelog CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 기존 테이블. CONVERT TO 는 저장된 값을 새 문자셋으로 변환해 옮깁니다.
ALTER TABLE users             CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE categories        CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE posts             CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE posts_tags        CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE photos_categories CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE photos            CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE photos_tags       CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ── 3. 적용 후 확인 ────────────────────────────────────────────────
-- 아무 행도 나오지 않아야 합니다.
SELECT TABLE_NAME, COLUMN_NAME, CHARACTER_SET_NAME, COLLATION_NAME
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE()
   AND CHARACTER_SET_NAME IS NOT NULL
   AND CHARACTER_SET_NAME <> 'utf8mb4'
 ORDER BY TABLE_NAME, ORDINAL_POSITION;
