-- =====================================================================
-- 수동 적용 SQL: 대시보드 조회 성능 개선용 인덱스 추가
-- 목적: 대시보드 초기 진입 시 반복 호출되는 조회 API의 user/date/status
--       조건 검색과 집계 쿼리 비용을 줄입니다.
-- 주의:
-- 1) 이 프로젝트는 Flyway/Liquibase 자동 마이그레이션을 사용하지 않습니다.
--    prod 프로필은 ddl-auto=validate 이므로 배포 전 운영 DB에 먼저 수동 적용해야 합니다.
-- 2) MySQL은 CREATE INDEX IF NOT EXISTS를 지원하지 않는 환경이 많으므로,
--    운영 반영 전 동일 인덱스 존재 여부를 먼저 확인해야 합니다.
-- 3) uk_point_log_user_reason_date 생성 전 QUEST 로그 중복 여부를 먼저 확인해야 합니다.
--
-- 적용 전 점검 예시:
-- SHOW INDEX FROM point_log WHERE Key_name = 'uk_point_log_user_reason_date';
-- SELECT user_id, reason, awarded_date, COUNT(*) AS duplicate_count
-- FROM point_log
-- WHERE awarded_date IS NOT NULL
-- GROUP BY user_id, reason, awarded_date
-- HAVING COUNT(*) > 1;
-- =====================================================================

-- 잔디 그래프, 요일별 작성, 오늘의 목표, 개인 통계
-- watered_at을 두 번째 컬럼에 두어 날짜 범위 조회를 우선 지원하고,
-- pot_id와 content_length까지 포함해 "오늘 물 준 화분" 판별과 글자 수 집계를 함께 커버합니다.
CREATE INDEX idx_watering_log_user_watered_at
    ON watering_log (user_id, watered_at, pot_id, content_length);

-- 화분 상세/집계 보조
CREATE INDEX idx_watering_log_pot_watered_at
    ON watering_log (pot_id, watered_at);

-- 화분 상세 최신 물주기 1건 조회
CREATE INDEX idx_watering_log_user_pot_watered_at
    ON watering_log (user_id, pot_id, watered_at);

-- 내 화분 목록, 주제 비율
CREATE INDEX idx_pot_user_id
    ON pot (user_id);

-- 내 화분 목록의 현재 활성 식물 조회
CREATE INDEX idx_plant_item_pot_harvested
    ON plant_item (pot_id, is_harvested);

CREATE INDEX idx_plant_item_user_harvested
    ON plant_item (user_id, is_harvested);

CREATE INDEX idx_posts_user_status_id
    ON posts (user_id, status, id);

-- 화분별 TIL 집계, 월별 관심사 조회
CREATE INDEX idx_til_pot_published_at
    ON til (pot_id, published_at);

CREATE INDEX idx_til_published_at
    ON til (published_at);

-- 관심사 흐름 태그 조인
CREATE INDEX idx_til_tag_til_id
    ON til_tag (til_id);

CREATE INDEX idx_til_tag_tag_id
    ON til_tag (tag_id);

-- 포인트 현황, 오늘의 목표 중복 지급 확인
CREATE INDEX idx_point_log_user_amount
    ON point_log (user_id, amount);

-- 오늘의 목표 포인트 중복 지급 방지
-- awarded_date가 NULL인 AI 사용 로그는 MySQL의 UNIQUE + NULL 허용 규칙상 여러 번 저장 가능합니다.
CREATE UNIQUE INDEX uk_point_log_user_reason_date
    ON point_log (user_id, reason, awarded_date);

CREATE INDEX idx_point_log_user_awarded_reason
    ON point_log (user_id, awarded_date, reason);
