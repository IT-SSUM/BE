-- 기준 데이터. 서버가 뜰 때마다 실행되므로 이미 있으면 건너뛴다 (INSERT IGNORE, UQ·PK 기준)

-- 파일럿 지역: 경기 부천시 (Region.PILOT_REGION_ID = 1). 기부자 활동 지역·물품 지역은 이 지역으로 고정한다
INSERT IGNORE INTO regions (region_id, sido, sigungu) VALUES (1, '경기', '부천시');

-- 물품 카테고리: Figma 「카테고리 선택」 화면의 7개 + 탐색 필터의 미사용 소모품(#50). 추천은 같은 카테고리끼리 묶는다 (ID 고정, 노출 순서 = ID)
INSERT IGNORE INTO item_categories (category_id, name, sort_order) VALUES
    (1, '생활용품', 1),
    (2, '의류·잡화', 2),
    (3, '가구', 3),
    (4, '주방용품', 4),
    (5, '도서·문구', 5),
    (6, '유아·아동용품', 6),
    (7, '기타', 7),
    (8, '미사용 소모품', 8);
