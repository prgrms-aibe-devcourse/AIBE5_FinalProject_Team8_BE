-- =====================================================================
-- 마이그레이션: 화분당 활성 PlantItem 중복 정리 + 유니크 인덱스 생성
-- 목적: 화분(pot_id)별 활성(isHarvested=false/null) PlantItem이 2개 이상인
--       데이터를 정리하고, 이후 중복 삽입을 DB 수준에서 차단합니다.
-- 실행 전 확인: 아래 SELECT로 중복 여부를 먼저 확인하세요.
-- =====================================================================

-- [사전 확인] 중복 활성 PlantItem 조회
-- SELECT pot_id, COUNT(*) AS cnt
-- FROM plant_item
-- WHERE is_harvested = 0 OR is_harvested IS NULL
-- GROUP BY pot_id
-- HAVING cnt > 1;

-- =====================================================================
-- Step 1. 중복 활성 PlantItem 정리
--         pot_id별 MIN(id)만 남기고 나머지 삭제
--         (ORDER BY id ASC 기준과 일관성 유지)
-- =====================================================================
DELETE FROM plant_item
WHERE (is_harvested = 0 OR is_harvested IS NULL)
  AND id NOT IN (
      SELECT min_id FROM (
          SELECT MIN(id) AS min_id
          FROM plant_item
          WHERE is_harvested = 0 OR is_harvested IS NULL
          GROUP BY pot_id
      ) AS keep_targets
  );

-- =====================================================================
-- Step 2. 유니크 인덱스 생성
--         is_harvested=false/null 인 경우에만 pot_id 유니크 적용
--         is_harvested=true 인 수확 완료 항목은 여러 개 허용 (NULL 무시)
--         MySQL 8.0.13+ 함수형 인덱스 사용
-- =====================================================================
CREATE UNIQUE INDEX ux_plant_item_one_active_per_pot
    ON plant_item ((IF(is_harvested = 0 OR is_harvested IS NULL, pot_id, NULL)));
