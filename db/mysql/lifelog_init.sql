-- =============================================================================
-- lifelog / MySQL 초기화 통합 스크립트
-- =============================================================================
-- 빈 MySQL 인스턴스 하나를 lifelog 가 바로 기동할 수 있는 상태로 만듭니다.
--   1. 데이터베이스 생성
--   2. 애플리케이션·마이그레이션 계정 생성 및 권한 부여
--   3. 테이블 7개 + 인덱스·제약조건 생성
--   4. 초기 관리자 계정과 카테고리 시드
--
-- 실행 (관리자 계정으로, 데이터베이스 지정 없이):
--   mysql -h <host> -P <port> -u <admin> -p < db/mysql/lifelog_init.sql
--
--   로컬 컨테이너:
--     docker exec -i <mysql-container> mysql -uroot -p<pw> < db/mysql/lifelog_init.sql
--   Aiven:
--     mysql -h <svc>.aivencloud.com -P <port> -u avnadmin -p --ssl-mode=REQUIRED \
--           < db/mysql/lifelog_init.sql
--
-- 전 구간 멱등합니다. 여러 번 실행해도 기존 데이터를 지우지 않습니다.
--
-- -----------------------------------------------------------------------------
-- 실행 전에 반드시 바꿀 것
-- -----------------------------------------------------------------------------
--   @app_password  / @ddl_password  : 아래 SET 문의 플레이스홀더
--   @admin_email   / @admin_password_hash : 관리자 계정
--
-- 이 저장소는 공개 라이선스입니다. 실제 비밀번호·이메일을 커밋하지 마십시오.
--
-- -----------------------------------------------------------------------------
-- 주의: 아래 DDL 은 Hibernate 7.2.1 + MariaDBDialect 가 엔티티에서 생성한 결과입니다.
--       app 은 ddl-auto: validate 로 기동하므로 타입이 하나라도 어긋나면 기동 자체가
--       실패합니다. 특히 다음 세 가지를 임의로 바꾸지 마십시오.
--         - posts.status 는 네이티브 ENUM 입니다 (VARCHAR 아님). 값 나열 순서도 유지
--         - is_active 는 BIT 입니다 (BOOLEAN/TINYINT 아님). 삽입 시 b'1'
--         - 타임스탬프는 DATETIME(6) 입니다 (마이크로초 자리까지)
-- =============================================================================

SET NAMES utf8mb4;


-- #############################################################################
-- 0. 치환 값
-- #############################################################################
-- MySQL 의 CREATE USER 는 비밀번호에 변수를 쓸 수 없어, 계정 비밀번호만 아래
-- CREATE USER 문에서 직접 교체해야 합니다. 시드 값은 변수로 처리합니다.

SET @admin_email         = 'admin@example.com';
SET @admin_name          = 'admin';
SET @admin_display_name  = 'Administrator';
-- 평문 'ChangeMe!2026' 의 BCrypt 해시입니다. 로그인 직후 반드시 교체하십시오.
-- 새 해시 생성 (이 저장소의 spring-security-crypto 사용):
--   CP=$(find ~/.gradle/caches -name 'spring-security-crypto-*.jar' | head -1)
--   jshell --class-path "$CP" -s - <<'EOF'
--   System.out.println(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("새비밀번호"));
--   EOF
SET @admin_password_hash = '$2a$10$nM21U.QCqZ53J8XnmC.fKubBadLJatKB4mK0FBqrJ2rGDhoPBxVCW';


-- #############################################################################
-- 1. 데이터베이스
-- #############################################################################
-- utf8mb4 가 아니면 이모지(사진 카테고리 icon)와 일부 한글이 저장되지 않습니다.
CREATE DATABASE IF NOT EXISTS lifelog
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;


-- #############################################################################
-- 2. 계정과 권한
-- #############################################################################
-- Aiven 등 관리형 MySQL 에서 콘솔로 사용자를 만들었다면 CREATE USER 두 줄은
-- 건너뛰고 GRANT 만 실행하십시오.

-- -----------------------------------------------------------------------------
-- 2-1. 애플리케이션 계정 — 평시 런타임
-- -----------------------------------------------------------------------------
-- app 이 쓰는 계정입니다. DDL 권한은 주지 않습니다. 애플리케이션이 스키마를 바꿀
-- 일이 없고(ddl-auto: validate), 사고로 테이블을 날리는 경로를 없애기 위함입니다.
--
-- 조회수 동기화(PostViewCountSyncService)가 posts.view_count 를 UPDATE 하는데,
-- 아래 스키마 단위 UPDATE 권한에 포함되어 있어 별도 GRANT 가 필요 없습니다.
CREATE USER IF NOT EXISTS 'lifelog_app'@'%'
    IDENTIFIED BY 'CHANGE_ME_APP_PASSWORD';

GRANT SELECT, INSERT, UPDATE, DELETE ON lifelog.* TO 'lifelog_app'@'%';

-- -----------------------------------------------------------------------------
-- 2-2. 마이그레이션 계정 — 스키마 변경 시에만
-- -----------------------------------------------------------------------------
-- 이 스크립트를 관리자 계정으로 돌린 뒤, 이후의 스키마 변경은 이 계정으로 합니다.
CREATE USER IF NOT EXISTS 'lifelog_ddl'@'%'
    IDENTIFIED BY 'CHANGE_ME_DDL_PASSWORD';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP, INDEX, REFERENCES
    ON lifelog.* TO 'lifelog_ddl'@'%';

-- GRANT 는 즉시 반영되므로 FLUSH PRIVILEGES 는 필요하지 않습니다.
-- Aiven 의 avnadmin 은 RELOAD 권한이 없어 오히려 오류가 날 수 있습니다.


-- #############################################################################
-- 3. 테이블 · 인덱스
-- #############################################################################
USE lifelog;

SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- 3-1. users — 사용자 계정 및 프로필
-- -----------------------------------------------------------------------------
-- 로그인 ID 는 email 입니다 (UserRepository.findByEmail).
-- 권한 컬럼은 없습니다. @AdminRequired 는 유효한 JWT 또는 세션의 userSeq 보유만
-- 확인하므로, 여기 등록된 계정으로 로그인하면 관리자 기능을 쓸 수 있습니다.
CREATE TABLE IF NOT EXISTS users (
    user_seq          BIGINT       NOT NULL AUTO_INCREMENT,
    email             VARCHAR(100) NOT NULL,
    name              VARCHAR(50)  NOT NULL,
    password_hash     VARCHAR(255) NOT NULL,
    display_name      VARCHAR(100) NOT NULL,
    bio               TEXT,
    profile_image_url VARCHAR(500),
    github_url        VARCHAR(200),
    linkedin_url      VARCHAR(200),
    is_active         BIT          NOT NULL,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    PRIMARY KEY (user_seq),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_name  UNIQUE (name),
    INDEX idx_email (email),
    INDEX idx_name  (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 3-2. categories — 계층형 게시글 카테고리 (self-referencing)
-- -----------------------------------------------------------------------------
-- parent_category_id 로 트리를 만듭니다. PostsQueryRepository 가 WITH RECURSIVE
-- 로 하위 카테고리를 따라가므로 자기참조 값이 끊기지 않게 유지하십시오.
CREATE TABLE IF NOT EXISTS categories (
    category_seq       BIGINT       NOT NULL AUTO_INCREMENT,
    category_name      VARCHAR(100) NOT NULL,
    slug               VARCHAR(100) NOT NULL,
    description        TEXT,
    parent_category_id BIGINT,
    display_order      INTEGER      NOT NULL,
    is_active          BIT          NOT NULL,
    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,
    PRIMARY KEY (category_seq),
    CONSTRAINT uk_categories_category_name UNIQUE (category_name),
    CONSTRAINT uk_categories_slug          UNIQUE (slug),
    INDEX idx_slug   (slug),
    INDEX idx_parent (parent_category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 3-3. posts — 블로그 게시글
-- -----------------------------------------------------------------------------
-- status 는 네이티브 ENUM 입니다. Hibernate 7 + MariaDBDialect 가
-- @Enumerated(EnumType.STRING) 을 ENUM 으로 매핑하므로 VARCHAR 로 바꾸면
-- validate 가 실패합니다. 값 나열은 Hibernate 가 생성한 알파벳 순서입니다.
--
-- 삭제는 물리 삭제가 아니라 status = 'ARCHIVED' 입니다.
CREATE TABLE IF NOT EXISTS posts (
    post_seq         BIGINT       NOT NULL AUTO_INCREMENT,
    user_seq         BIGINT       NOT NULL,
    category_seq     BIGINT,
    title            VARCHAR(200) NOT NULL,
    slug             VARCHAR(200) NOT NULL,
    summary          TEXT,
    content          LONGTEXT     NOT NULL,
    markdown_content LONGTEXT,
    thumbnail_url    VARCHAR(500),
    status           ENUM ('ARCHIVED','DRAFT','PUBLISHED') NOT NULL,
    view_count       INTEGER      NOT NULL,
    published_at     DATETIME(6),
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    PRIMARY KEY (post_seq),
    CONSTRAINT uk_posts_slug UNIQUE (slug),
    INDEX idx_slug         (slug),
    INDEX idx_status       (status),
    INDEX idx_published_at (published_at),
    INDEX idx_user_seq     (user_seq),
    INDEX idx_category_seq (category_seq),
    CONSTRAINT fk_posts_category
        FOREIGN KEY (category_seq) REFERENCES categories (category_seq)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 3-4. posts_tags — 게시글-태그 (복합키 @IdClass)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS posts_tags (
    post_seq   BIGINT       NOT NULL,
    tag_seq    INTEGER      NOT NULL,
    tag        VARCHAR(100) NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (post_seq, tag_seq)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 3-5. photos_categories — 사진 카테고리
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS photos_categories (
    category_seq  BIGINT       NOT NULL AUTO_INCREMENT,
    category_name VARCHAR(100) NOT NULL,
    icon          VARCHAR(255),
    is_active     BIT          NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (category_seq),
    CONSTRAINT uk_photos_categories_category_name UNIQUE (category_name),
    INDEX idx_name (category_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 3-6. photos — 사진 아카이브 (실제 이미지는 Google Drive, 여기엔 경로·EXIF 만)
-- -----------------------------------------------------------------------------
-- gps_latitude / gps_longitude 는 Hibernate 가 생성하는 decimal(38,2) 대신
-- decimal(10,7) / decimal(11,7) 로 선언했습니다. 엔티티에 precision/scale 선언이
-- 없어 기본값이 소수점 2자리가 되는데, 그대로 두면 위경도가 약 1.1km 단위로
-- 뭉개집니다. Hibernate 의 검증은 DECIMAL 자릿수를 비교하지 않아 validate 는
-- 통과합니다. 근본 수정은 Photo 엔티티에 precision/scale 을 선언하는 것입니다.
CREATE TABLE IF NOT EXISTS photos (
    photo_seq         BIGINT        NOT NULL AUTO_INCREMENT,
    user_seq          BIGINT        NOT NULL,
    title             VARCHAR(200)  NOT NULL,
    caption           TEXT,
    category_seq      BIGINT,
    image_url         VARCHAR(500)  NOT NULL,
    thumbnail_url     VARCHAR(500),
    like_count        INTEGER       NOT NULL,
    exif_maker        VARCHAR(100),
    exif_model        VARCHAR(100),
    exif_aperture     VARCHAR(20),
    exif_shutter      VARCHAR(20),
    exif_iso          VARCHAR(20),
    exif_focal_length BIGINT,
    exif_lens         VARCHAR(200),
    exif_flash        VARCHAR(3),
    gps_latitude      DECIMAL(10,7),
    gps_longitude     DECIMAL(11,7),
    shot_at           DATETIME(6),
    is_active         BIT           NOT NULL,
    created_at        DATETIME(6)   NOT NULL,
    updated_at        DATETIME(6)   NOT NULL,
    PRIMARY KEY (photo_seq),
    INDEX idx_user_seq     (user_seq),
    INDEX idx_category_seq (category_seq),
    INDEX idx_created_at   (created_at),
    CONSTRAINT fk_photos_category
        FOREIGN KEY (category_seq) REFERENCES photos_categories (category_seq)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 3-7. photos_tags — 사진-태그 (복합키 @IdClass)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS photos_tags (
    photo_seq  BIGINT       NOT NULL,
    tag_seq    INTEGER      NOT NULL,
    tag        VARCHAR(100) NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (photo_seq, tag_seq),
    INDEX idx_tag (tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;


-- #############################################################################
-- 4. 초기 데이터
-- #############################################################################

-- -----------------------------------------------------------------------------
-- 4-1. 관리자 계정
-- -----------------------------------------------------------------------------
-- is_active 는 BIT 이므로 b'1' 로 넣습니다.
INSERT INTO users (
    email, name, password_hash, display_name,
    bio, profile_image_url, github_url, linkedin_url,
    is_active, created_at, updated_at
) VALUES (
    @admin_email,
    @admin_name,
    @admin_password_hash,
    @admin_display_name,
    NULL, NULL, NULL, NULL,
    b'1', NOW(6), NOW(6)
)
ON DUPLICATE KEY UPDATE updated_at = NOW(6);

-- -----------------------------------------------------------------------------
-- 4-2. 게시글 카테고리
-- -----------------------------------------------------------------------------
-- 운영에서 쓰던 카테고리명은 코드에 없고 DB 에만 있었습니다. 아래는 자리를 잡아둔
-- 예시입니다. 구 DB 덤프가 있으면 이 블록을 지우고 덤프를 쓰십시오.
INSERT INTO categories (
    category_name, slug, description, parent_category_id,
    display_order, is_active, created_at, updated_at
) VALUES
    ('개발', 'dev',    '개발 관련 글', NULL, 1, b'1', NOW(6), NOW(6)),
    ('일상', 'life',   '일상 기록',    NULL, 2, b'1', NOW(6), NOW(6)),
    ('여행', 'travel', '여행 기록',    NULL, 3, b'1', NOW(6), NOW(6))
ON DUPLICATE KEY UPDATE updated_at = NOW(6);

-- -----------------------------------------------------------------------------
-- 4-3. 사진 카테고리
-- -----------------------------------------------------------------------------
-- icon 은 이모지입니다. utf8mb4 이므로 4바이트 문자가 그대로 들어갑니다.
INSERT INTO photos_categories (
    category_name, icon, is_active, created_at, updated_at
) VALUES
    ('풍경', '🏞️', b'1', NOW(6), NOW(6)),
    ('인물', '🧑',  b'1', NOW(6), NOW(6)),
    ('일상', '☕',  b'1', NOW(6), NOW(6))
ON DUPLICATE KEY UPDATE updated_at = NOW(6);


-- #############################################################################
-- 5. 결과 확인
-- #############################################################################
SELECT '테이블' AS 구분, TABLE_NAME AS 이름, TABLE_COLLATION AS 콜레이션, ENGINE AS 엔진
  FROM information_schema.TABLES
 WHERE TABLE_SCHEMA = 'lifelog'
 ORDER BY TABLE_NAME;

SELECT '계정' AS 구분, user AS 이름, host AS 호스트
  FROM mysql.user
 WHERE user LIKE 'lifelog%'
 ORDER BY user;

SELECT '관리자' AS 구분, user_seq, email, name, display_name, is_active FROM users;
SELECT '글 카테고리' AS 구분, category_seq, category_name, slug, display_order
  FROM categories ORDER BY display_order;
SELECT '사진 카테고리' AS 구분, category_seq, category_name, icon FROM photos_categories;

-- =============================================================================
-- 다음 단계
-- =============================================================================
--  1. lifelog_app / lifelog_ddl 비밀번호를 교체했는지 확인
--  2. 애플리케이션에 환경변수 주입 (docker/lifelog.env.example 참고)
--       MYSQL_HOST / MYSQL_PORT / MYSQL_DATABASE / MYSQL_USERNAME / MYSQL_PASSWORD
--       TLS 가 없는 상대라면 MYSQL_SSL_MODE=DISABLED
--  3. MongoDB 콘텐츠 복구 — db/mongodb/01_init.js, content-documents.ndjson
--  4. 로그인 후 관리자 비밀번호 즉시 교체
-- =============================================================================
