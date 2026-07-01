# DB 변경 SQL 적용 안내

이 디렉터리의 SQL 파일은 운영 DB에 자동 적용되지 않는 수동 적용 스크립트입니다.

현재 프로젝트는 Flyway/Liquibase를 사용하지 않고, `prod` 프로필의 Hibernate 설정은
`ddl-auto: validate`입니다. 따라서 배포 전에 인프라/배포 담당자가 대상 DB에 SQL을
직접 적용해야 합니다.

## 적용 전 확인

- 같은 이름의 인덱스가 이미 존재하는지 확인합니다.
- `UNIQUE INDEX`를 추가하는 경우 기존 데이터에 중복이 없는지 먼저 확인합니다.
- 운영 반영 전 로컬 또는 스테이징 DB에서 실행 계획과 애플리케이션 기동을 확인합니다.

## 현재 파일

- `add_refresh_token_rotation_columns.sql`: Refresh Token 회전 컬럼 추가
- `add_dashboard_performance_indexes.sql`: 대시보드/정원/포인트 조회 성능 개선 인덱스 추가
