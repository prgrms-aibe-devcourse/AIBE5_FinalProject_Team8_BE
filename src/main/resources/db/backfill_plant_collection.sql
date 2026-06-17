-- ============================================================
-- backfill_plant_collection.sql
--
-- 대상: PR #169(씨앗 배정 정책) 이전에 pot을 생성한 기존 계정
-- 증상: plant_collection 레코드 없음 → 수확 시 "해금된 씨앗이 없습니다." 오류
-- 처리: plant_item 이력 기반으로 유저별 해금 씨앗을 INSERT IGNORE
--
-- 실행 전 검증 (영향 대상 유저 수 확인):
--   SELECT COUNT(DISTINCT pi.user_id)
--   FROM plant_item pi
--   WHERE NOT EXISTS (
--       SELECT 1 FROM plant_collection pc WHERE pc.user_id = pi.user_id
--   );
--
-- 실행 후 검증:
--   SELECT user_id, COUNT(*) AS unlocked FROM plant_collection GROUP BY user_id ORDER BY user_id;
-- ============================================================

INSERT IGNORE INTO plant_collection (user_id, plant_id, created_at)
SELECT DISTINCT
    pi.user_id,
    pi.plant_id,
    NOW()
FROM plant_item pi
INNER JOIN plant p ON p.id = pi.plant_id
    AND p.growth_stage = 'Seed';
