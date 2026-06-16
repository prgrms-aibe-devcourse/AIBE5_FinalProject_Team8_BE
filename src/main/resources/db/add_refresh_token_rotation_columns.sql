-- =====================================================================
-- 마이그레이션: Refresh Token Rotation + Grace Period 컬럼 추가
-- 목적: Refresh Token 재발급 시 Token Rotation을 유지하면서,
--       동시 재발급 요청은 짧은 유예 시간 동안 같은 대체 토큰으로 응답합니다.
-- 주의: prod 프로필은 ddl-auto=validate 이므로 배포 전 운영 DB에 먼저 적용해야 합니다.
-- =====================================================================

ALTER TABLE refresh_tokens
    ADD COLUMN rotated_at DATETIME NULL,
    ADD COLUMN grace_expires_at DATETIME NULL,
    ADD COLUMN replacement_token VARCHAR(512) NULL;
