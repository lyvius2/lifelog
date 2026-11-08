-- H2 인메모리 DB(default 프로파일)용 스키마.
--
-- 예전에는 Hibernate 의 ddl-auto: create-drop 이 엔티티에서 테이블을 만들어
-- 주었습니다. JPA 를 걷어냈으므로 스키마를 직접 둡니다.
-- spring.sql.init.mode: embedded 라서 H2 같은 내장 DB 에서만 실행됩니다
-- (live / dev 의 MySQL 은 db/mysql/lifelog_init.sql 로 따로 구축합니다).
--
-- db/mysql/lifelog_init.sql 의 3절에서 가져왔습니다. 엔티티나 쿼리의 컬럼이
-- 바뀌면 양쪽을 함께 고치십시오. H2 는 MODE=MySQL 로 띄우므로 MySQL 문법을
-- 그대로 씁니다.
--
-- 주의: 인덱스 이름에 테이블명을 접두사로 붙였습니다.
-- MySQL 은 인덱스 이름이 테이블 단위라 categories.idx_slug 와 posts.idx_slug 가
-- 공존하지만, H2 는 스키마 전역이라 두 번째에서 "Index IDX_SLUG already exists" 로
-- 실패합니다. db/mysql/lifelog_init.sql 쪽은 원래 이름을 그대로 둡니다.
--

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
    INDEX users_idx_email (email),
    INDEX users_idx_name  (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
    INDEX categories_idx_slug   (slug),
    INDEX categories_idx_parent (parent_category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
    INDEX posts_idx_slug         (slug),
    INDEX posts_idx_status       (status),
    INDEX posts_idx_published_at (published_at),
    INDEX posts_idx_user_seq     (user_seq),
    INDEX posts_idx_category_seq (category_seq),
    CONSTRAINT fk_posts_category
        FOREIGN KEY (category_seq) REFERENCES categories (category_seq)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS posts_tags (
    post_seq   BIGINT       NOT NULL,
    tag_seq    INTEGER      NOT NULL,
    tag        VARCHAR(100) NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (post_seq, tag_seq)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS photos_categories (
    category_seq  BIGINT       NOT NULL AUTO_INCREMENT,
    category_name VARCHAR(100) NOT NULL,
    icon          VARCHAR(255),
    is_active     BIT          NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (category_seq),
    CONSTRAINT uk_photos_categories_category_name UNIQUE (category_name),
    INDEX photos_categories_idx_name (category_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
    INDEX photos_idx_user_seq     (user_seq),
    INDEX photos_idx_category_seq (category_seq),
    INDEX photos_idx_created_at   (created_at),
    CONSTRAINT fk_photos_category
        FOREIGN KEY (category_seq) REFERENCES photos_categories (category_seq)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS photos_tags (
    photo_seq  BIGINT       NOT NULL,
    tag_seq    INTEGER      NOT NULL,
    tag        VARCHAR(100) NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (photo_seq, tag_seq),
    INDEX photos_tags_idx_tag (tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

