-- 기준 데이터. 서버가 뜰 때마다 실행되므로 이미 있으면 건너뛴다 (INSERT IGNORE, UQ·PK 기준)

-- 파일럿 지역: 경기 부천시 (Region.PILOT_REGION_ID = 1). 기부자 활동 지역·물품 지역은 이 지역으로 고정한다
INSERT IGNORE INTO regions (region_id, sido, sigungu) VALUES (1, '경기', '부천시');
