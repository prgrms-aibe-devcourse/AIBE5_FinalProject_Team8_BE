-- ============================================================
-- add_til_images_table.sql
--
-- 목적: TIL 본문 이미지 저장을 위한 til_images 테이블 생성
-- 대상: ERD Image 테이블 구현 (post_id → posts.id FK)
--
-- 적용 환경:
--   - local  프로필: ddl-auto=update → Hibernate가 자동 생성 (이 SQL 불필요)
--   - prod   프로필: ddl-auto=validate → 반드시 이 SQL을 먼저 실행해야 함
--
-- 실행 전 검증:
--   SHOW TABLES LIKE 'til_images';
--
-- 실행 후 검증:
--   DESCRIBE til_images;
--   SELECT * FROM til_images LIMIT 5;
-- ============================================================

CREATE TABLE IF NOT EXISTS til_images (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '이미지 레코드 고유 ID',
    post_id     BIGINT       NOT NULL                COMMENT 'TIL post ID (posts.id FK)',
    url         VARCHAR(500) NOT NULL                COMMENT 'S3 이미지 공개 URL',
    image_order INT          NOT NULL DEFAULT 0      COMMENT '본문 삽입 순서 (0부터 시작)',
    created_at  DATETIME     NOT NULL                COMMENT '레코드 생성 시각',
    PRIMARY KEY (id),
    CONSTRAINT fk_til_images_post
        FOREIGN KEY (post_id) REFERENCES posts (id)
        ON DELETE CASCADE,
    INDEX idx_til_images_post_id (post_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='TIL 본문 삽입 이미지 저장 테이블 (ERD: Image)';
