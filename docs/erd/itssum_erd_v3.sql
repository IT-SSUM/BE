-- =====================================================================
-- IT:SSUM ERD v3 (MySQL 8.0)  — API 명세 + Figma 화면 대조 반영본
-- 기부자 ↔ 복지기관 물품 기부 매칭 서비스
--
-- [1] CREATE TABLE + PK
-- [2] FK
-- [3] UNIQUE / INDEX
--
-- 테이블 한글명은 테이블 COMMENT로 지정
-- enum 성격 컬럼은 JPA @Enumerated(EnumType.STRING) 전제로 VARCHAR + COMMENT로 값 명시
--
-- v2 → v3 변경
--   * 모든 테이블에 한글 COMMENT, utf8mb4 지정 (채팅 이모지 대비)
--   + notification_settings   : GET/PATCH /api/users/me/notification-settings
--   + push_tokens             : POST/DELETE /api/users/me/push-tokens (후순위)
--   + password_reset_tokens   : Figma A13·A14 비밀번호 재설정(메일 링크)
--   * donors        : birth_date 필수, region_id(활동 지역·선택), introduction(기부자 소개) 추가
--   * organizations : introduction(기관 소개) 추가
--   * donation_items: pickup_address(물품 상세 위치) 추가
--   * needed_items  : region_id(수령 지역) 추가, receive_address 선택으로 변경,
--                     desired_condition 필수, reference_image_url(참고 사진) 추가
--   * matches       : delivery_method(직접 전달/택배), scheduled_at(전달 일정) 추가
--   * chat_messages : message_type에 APPOINTMENT(전달 약속 카드) 추가
-- =====================================================================


-- =====================================================================
-- [1] TABLES
-- =====================================================================

-- ---------- 회원 ----------
CREATE TABLE `users` (
  `user_id`     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '회원 ID',
  `email`       VARCHAR(255) NOT NULL COMMENT '로그인 이메일',
  `password`    VARCHAR(255) NOT NULL COMMENT 'BCrypt 해시',
  `role`        VARCHAR(20)  NOT NULL COMMENT 'DONOR | ORGANIZATION (가입 유형 선택)',
  `status`      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE | WITHDRAWN',
  `created_at`  DATETIME     NOT NULL COMMENT '가입 일시',
  `updated_at`  DATETIME     NOT NULL COMMENT '수정 일시',
  `deleted_at`  DATETIME     NULL     COMMENT '탈퇴 일시 (soft delete)',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='회원';

-- 회원가입 3/3 필수 약관 동의 + 기관 가입 자격 확인(A16) 이력
CREATE TABLE `terms_agreements` (
  `agreement_id`  BIGINT      NOT NULL AUTO_INCREMENT COMMENT '동의 이력 ID',
  `user_id`       BIGINT      NOT NULL COMMENT '회원 ID',
  `terms_type`    VARCHAR(30) NOT NULL COMMENT 'SERVICE(서비스 이용약관) | PRIVACY(개인정보 수집·이용) | ORG_ELIGIBILITY(기관 가입 자격 확인)',
  `terms_version` VARCHAR(20) NOT NULL COMMENT '동의한 약관 버전',
  `agreed_at`     DATETIME    NOT NULL COMMENT '동의 일시',
  PRIMARY KEY (`agreement_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='약관 동의 이력';

-- GET/PATCH /api/donors/me
-- Figma 기부자 회원가입 1/3: 이름·생년월일·전화번호 필수, 활동 지역 선택
CREATE TABLE `donors` (
  `user_id`      BIGINT       NOT NULL COMMENT '회원 ID (users PK 공유)',
  `region_id`    BIGINT       NULL     COMMENT '활동 지역 (선택, 마이페이지 "최윤서님 · 서울 구로구")',
  `name`         VARCHAR(30)  NOT NULL COMMENT '이름',
  `birth_date`   DATE         NOT NULL COMMENT '생년월일',
  `phone`        VARCHAR(20)  NOT NULL COMMENT '전화번호',
  `introduction` VARCHAR(255) NULL     COMMENT '기부자 소개 (기관 화면의 기부 물품 상세에 노출)',
  `created_at`   DATETIME     NOT NULL COMMENT '생성 일시',
  `updated_at`   DATETIME     NOT NULL COMMENT '수정 일시',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='기부자';

-- GET/PATCH /api/organizations/me, GET /api/organizations/{organizationId}
-- GET /api/organizations/me/verification → verification_status, verified_at
CREATE TABLE `organizations` (
  `user_id`             BIGINT        NOT NULL COMMENT '회원 ID (users PK 공유, API의 organizationId)',
  `name`                VARCHAR(100)  NOT NULL COMMENT '기관명',
  `representative_name` VARCHAR(50)   NOT NULL COMMENT '대표자명 (NTS 진위확인 p_nm)',
  `business_number`     CHAR(10)      NOT NULL COMMENT '사업자번호 10자리, 하이픈 제외 (NTS b_no)',
  `opening_date`        DATE          NOT NULL COMMENT '개업일자 (NTS start_dt)',
  `phone`               VARCHAR(20)   NOT NULL COMMENT '기관 연락처',
  `address`             VARCHAR(255)  NOT NULL COMMENT '기관 주소',
  `introduction`        VARCHAR(255)  NULL     COMMENT '기관 소개 (필요 물품 상세·기관 상세에 노출)',
  `latitude`            DECIMAL(10,7) NULL     COMMENT '위도 (주소 지오코딩, 거리순 추천용)',
  `longitude`           DECIMAL(10,7) NULL     COMMENT '경도',
  `verification_status` VARCHAR(20)   NOT NULL DEFAULT 'VERIFIED' COMMENT 'VERIFIED | SUSPENDED(상태조회로 휴·폐업 감지)',
  `verified_at`         DATETIME      NOT NULL COMMENT '사업자 확인 일시',
  `created_at`          DATETIME      NOT NULL COMMENT '생성 일시',
  `updated_at`          DATETIME      NOT NULL COMMENT '수정 일시',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='복지기관';

-- 국세청 사업자등록정보 진위확인·상태조회 API 호출 이력 (기관 승인 openAPI)
CREATE TABLE `organization_verifications` (
  `verification_id`      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '인증 이력 ID',
  `organization_id`      BIGINT       NULL     COMMENT '기관 ID (가입 전 사업자 확인 단계에선 NULL, 가입 완료 시 연결)',
  `verification_type`    VARCHAR(20)  NOT NULL COMMENT 'VALIDATE(진위확인) | STATUS(상태조회)',
  `business_number`      CHAR(10)     NOT NULL COMMENT '요청 사업자번호 (b_no)',
  `valid_code`           CHAR(2)      NULL     COMMENT '진위확인 결과 valid: 01 일치 | 02 불일치',
  `business_status_code` CHAR(2)      NULL     COMMENT '상태 b_stt_cd: 01 계속 | 02 휴업 | 03 폐업',
  `tax_type`             VARCHAR(100) NULL     COMMENT '과세유형 메시지 (tax_type)',
  `closed_date`          DATE         NULL     COMMENT '폐업일 (end_dt)',
  `result`               VARCHAR(20)  NOT NULL COMMENT 'SUCCESS | FAIL | ERROR(API 장애)',
  `checked_at`           DATETIME     NOT NULL COMMENT '조회 일시',
  PRIMARY KEY (`verification_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='기관 사업자 인증 이력';

-- ---------- 기준 데이터 ----------
-- 지역 선택 화면(구 단위) + 거리순 추천의 기준 좌표
CREATE TABLE `regions` (
  `region_id`  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '지역 ID',
  `sido`       VARCHAR(20)   NOT NULL COMMENT '시·도 (서울)',
  `sigungu`    VARCHAR(30)   NOT NULL COMMENT '시·군·구 (구로구)',
  `latitude`   DECIMAL(10,7) NOT NULL COMMENT '구 중심 위도',
  `longitude`  DECIMAL(10,7) NOT NULL COMMENT '구 중심 경도',
  PRIMARY KEY (`region_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='지역';

-- GET /api/item-categories
-- (GET /api/item-conditions 는 condition_grade enum 값으로 응답 — 별도 테이블 없음)
CREATE TABLE `item_categories` (
  `category_id` BIGINT      NOT NULL AUTO_INCREMENT COMMENT '카테고리 ID',
  `name`        VARCHAR(50) NOT NULL COMMENT '카테고리명 (도서·문구, 생활용품 ...)',
  `sort_order`  INT         NOT NULL DEFAULT 0 COMMENT '노출 순서',
  PRIMARY KEY (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='물품 카테고리';

-- ---------- 기부 물품 (나눔) ----------
-- /api/donation-items, GET /api/donors/me/donation-items
CREATE TABLE `donation_items` (
  `donation_item_id`   BIGINT      NOT NULL AUTO_INCREMENT COMMENT '기부 물품 ID',
  `donor_id`           BIGINT      NOT NULL COMMENT '등록한 기부자',
  `category_id`        BIGINT      NOT NULL COMMENT '카테고리',
  `region_id`          BIGINT      NOT NULL COMMENT '물품 위치 (구 단위, 목록·상세에 공개)',
  `pickup_address`     VARCHAR(255) NULL    COMMENT '물품 상세 위치 (물품 수정 화면, 비공개·채팅에서 협의)',
  `item_name`          VARCHAR(50) NOT NULL COMMENT '품목 (어린이 도서, 생수 ...)',
  `quantity`           INT         NOT NULL COMMENT '총 수량',
  `unit`               VARCHAR(10) NOT NULL COMMENT '단위 (개, 권, 장, 벌 ...)',
  `reserved_quantity`  INT         NOT NULL DEFAULT 0 COMMENT '진행 수량 (수락~전달 중인 매칭 합계)',
  `completed_quantity` INT         NOT NULL DEFAULT 0 COMMENT '전달 완료 수량',
  `condition_grade`    VARCHAR(10) NOT NULL COMMENT '물품 상태: HIGH(상·미개봉) | MID(중·사용감 적음) | LOW(하·다회 사용)',
  `description`        TEXT        NULL     COMMENT '설명 (선택)',
  `status`             VARCHAR(20) NOT NULL DEFAULT 'SHARING' COMMENT 'SHARING(나눔중: 가능 수량 남음) | RESERVED(예약중: 남은 수량 0) | COMPLETED(전달 완료)',
  `created_at`         DATETIME    NOT NULL COMMENT '등록 일시',
  `updated_at`         DATETIME    NOT NULL COMMENT '수정 일시',
  `deleted_at`         DATETIME    NULL     COMMENT '삭제 일시 (soft delete)',
  PRIMARY KEY (`donation_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='기부 물품';

CREATE TABLE `donation_item_images` (
  `image_id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '이미지 ID',
  `donation_item_id` BIGINT       NOT NULL COMMENT '기부 물품 ID',
  `image_url`        VARCHAR(500) NOT NULL COMMENT 'S3 URL (최소 1장)',
  `sort_order`       INT          NOT NULL DEFAULT 0 COMMENT '0 = 대표 이미지',
  `created_at`       DATETIME     NOT NULL COMMENT '등록 일시',
  PRIMARY KEY (`image_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='기부 물품 이미지';

-- ---------- 필요 물품 (기관 모집) ----------
-- /api/needed-items, GET /api/organizations/me/needed-items
-- PATCH /api/needed-items/{id}/close → status CLOSED, closed_at
-- Figma 필요 물품 등록 1/2: 카테고리·품목·필요 수량·수령 지역 / 2/2: 희망 상태(필수)·필요한 이유·참고 사진
CREATE TABLE `needed_items` (
  `needed_item_id`      BIGINT        NOT NULL AUTO_INCREMENT COMMENT '필요 물품 ID',
  `organization_id`     BIGINT        NOT NULL COMMENT '등록한 기관',
  `category_id`         BIGINT        NOT NULL COMMENT '카테고리',
  `region_id`           BIGINT        NOT NULL COMMENT '수령 지역 (구 단위, 목록·필터에 공개)',
  `item_name`           VARCHAR(50)   NOT NULL COMMENT '품목',
  `required_quantity`   INT           NOT NULL COMMENT '필요 수량',
  `unit`                VARCHAR(10)   NOT NULL COMMENT '단위',
  `reserved_quantity`   INT           NOT NULL DEFAULT 0 COMMENT '예약(진행) 수량',
  `received_quantity`   INT           NOT NULL DEFAULT 0 COMMENT '수령 완료 수량',
  `desired_condition`   VARCHAR(10)   NOT NULL COMMENT '희망 최소 상태: HIGH | MID | LOW ("중 이상"처럼 이 등급 이상)',
  `description`         TEXT          NULL     COMMENT '필요한 이유 / 설명 (선택)',
  `reference_image_url` VARCHAR(500)  NULL     COMMENT '참고 사진 S3 URL (선택, 1장)',
  `receive_address`     VARCHAR(255)  NULL     COMMENT '수령 장소 상세 주소 (선택, 필요 나눔 수정 화면)',
  `latitude`            DECIMAL(10,7) NULL     COMMENT '수령 장소 위도',
  `longitude`           DECIMAL(10,7) NULL     COMMENT '수령 장소 경도',
  `status`              VARCHAR(20)   NOT NULL DEFAULT 'RECRUITING' COMMENT 'RECRUITING(모집중) | FULL(신규 제안 제한: 추가 필요 0) | CLOSED(모집 종료)',
  `closed_at`           DATETIME      NULL     COMMENT '모집 종료 일시',
  `created_at`          DATETIME      NOT NULL COMMENT '등록 일시',
  `updated_at`          DATETIME      NOT NULL COMMENT '수정 일시',
  `deleted_at`          DATETIME      NULL     COMMENT '삭제 일시 (soft delete)',
  PRIMARY KEY (`needed_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='필요 물품';

-- ---------- AI 추천 ----------
-- 기부 물품 → 추천 기관 순위 (POST/GET /api/donation-items/{id}/recommendations)
-- POST 재호출 시 기존 행 삭제 후 재생성 (UNIQUE donation_item_id + needed_item_id)
CREATE TABLE `organization_recommendations` (
  `recommendation_id` BIGINT       NOT NULL AUTO_INCREMENT COMMENT '추천 ID',
  `donation_item_id`  BIGINT       NOT NULL COMMENT '기준 기부 물품',
  `needed_item_id`    BIGINT       NOT NULL COMMENT '추천된 기관의 필요 물품',
  `organization_id`   BIGINT       NOT NULL COMMENT '추천된 기관 (목록 조회용)',
  `rank_no`           INT          NOT NULL COMMENT '추천 순위',
  `score`             DECIMAL(5,2) NULL     COMMENT '적합도 점수',
  `distance_km`       DECIMAL(6,2) NULL     COMMENT '거리 (거리순 정렬용)',
  `reason`            VARCHAR(255) NULL     COMMENT '추천 사유 (품목 수량 일치, 가까운 거리, 상태 적합 ...)',
  `created_at`        DATETIME     NOT NULL COMMENT '생성 일시',
  PRIMARY KEY (`recommendation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 기관 추천';

-- 필요 물품 → 추천 기부자(기부 물품) (POST/GET /api/needed-items/{id}/recommendations)
CREATE TABLE `donation_item_recommendations` (
  `recommendation_id` BIGINT       NOT NULL AUTO_INCREMENT COMMENT '추천 ID',
  `needed_item_id`    BIGINT       NOT NULL COMMENT '기준 필요 물품',
  `donation_item_id`  BIGINT       NOT NULL COMMENT '추천된 기부 물품',
  `rank_no`           INT          NOT NULL COMMENT '추천 순위',
  `score`             DECIMAL(5,2) NULL     COMMENT '적합도 점수',
  `distance_km`       DECIMAL(6,2) NULL     COMMENT '거리 (거리순 정렬용)',
  `reason`            VARCHAR(255) NULL     COMMENT '추천 사유',
  `created_at`        DATETIME     NOT NULL COMMENT '생성 일시',
  PRIMARY KEY (`recommendation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 기부 물품 추천';

-- ---------- 매칭 (기부 제안 / 나눔 요청 → 나눔 진행) ----------
-- POST /api/matches, GET /api/matches(/{id})
-- 상태 흐름 (Figma 응답 대기·나눔 연결 결과·나눔 진행 상세 기준)
--   REQUESTED(응답 대기) ─수락→ RESERVED(예약중) ─기부자 "나눔중으로 변경"→ SHARING(나눔중) ─기관 "수령 확인"→ COMPLETED
--   REQUESTED ─거절→ REJECTED / 진행 중 "나눔 취소·지원 취소" → CANCELED (수량 복원)
-- ※ API 명세엔 거절·나눔중 변경 엔드포인트가 없음 (/reserve, /complete, /cancel 만 있음)
CREATE TABLE `matches` (
  `match_id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '매칭 ID',
  `donation_item_id` BIGINT       NOT NULL COMMENT '기부 물품',
  `needed_item_id`   BIGINT       NOT NULL COMMENT '필요 물품 (제안·요청 모두 필요 물품에 연결)',
  `donor_id`         BIGINT       NOT NULL COMMENT '기부자 (목록 조회용 역정규화)',
  `organization_id`  BIGINT       NOT NULL COMMENT '기관 (목록 조회용 역정규화)',
  `requested_by`     VARCHAR(20)  NOT NULL COMMENT 'DONOR(기부 제안) | ORGANIZATION(나눔 요청)',
  `quantity`         INT          NOT NULL COMMENT '제안/요청 수량 (수락 시 양쪽 reserved_quantity에 반영)',
  `message`          VARCHAR(500) NULL     COMMENT '상대에게 전할 메시지',
  `status`           VARCHAR(20)  NOT NULL DEFAULT 'REQUESTED' COMMENT 'REQUESTED(응답 대기) | RESERVED(예약중: 수락·수량 확보) | SHARING(나눔중: 기부자 전달 시작) | COMPLETED(전달 완료: 기관 수령 확인) | REJECTED | CANCELED',
  `responded_at`     DATETIME     NULL     COMMENT '수락/거절 일시',
  `sharing_at`       DATETIME     NULL     COMMENT '나눔중 변경 일시',
  `completed_at`     DATETIME     NULL     COMMENT '수령 확인 일시',
  `delivery_method`  VARCHAR(20)  NULL     COMMENT '전달 방법: DIRECT(직접 전달) | PARCEL(택배) — 채팅에서 협의 후 확정',
  `scheduled_at`     DATETIME     NULL     COMMENT '전달 일정 (전달 약속 · 10월 9일 오후 2시)',
  `delivery_place`   VARCHAR(255) NULL     COMMENT '전달 장소 (구로지역아동센터 입구)',
  `receiver_name`    VARCHAR(30)  NULL     COMMENT '수령 담당자',
  `canceled_at`      DATETIME     NULL     COMMENT '취소 일시',
  `canceled_by`      BIGINT       NULL     COMMENT '취소/거절한 회원',
  `cancel_reason`    VARCHAR(255) NULL     COMMENT '취소/거절 사유',
  `created_at`       DATETIME     NOT NULL COMMENT '생성 일시',
  `updated_at`       DATETIME     NOT NULL COMMENT '수정 일시',
  PRIMARY KEY (`match_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='매칭';

-- ---------- 채팅 ----------
-- GET /api/chat-rooms(/{id}), GET /{id}/messages, STOMP /ws · /topic · /app
CREATE TABLE `chat_rooms` (
  `chat_room_id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '채팅방 ID',
  `match_id`             BIGINT       NOT NULL COMMENT '매칭 1건당 채팅방 1개 (수락 시 생성)',
  `last_message_content` VARCHAR(255) NULL     COMMENT '목록 미리보기용 마지막 메시지',
  `last_message_at`      DATETIME     NULL     COMMENT '목록 정렬용 마지막 메시지 시각',
  `created_at`           DATETIME     NOT NULL COMMENT '생성 일시',
  PRIMARY KEY (`chat_room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='채팅방';

-- PATCH /api/chat-rooms/{id}/read → last_read_message_id 갱신
CREATE TABLE `chat_participants` (
  `chat_participant_id`  BIGINT   NOT NULL AUTO_INCREMENT COMMENT '참여 ID',
  `chat_room_id`         BIGINT   NOT NULL COMMENT '채팅방',
  `user_id`              BIGINT   NOT NULL COMMENT '참여 회원 (기부자 또는 기관)',
  `last_read_message_id` BIGINT   NULL     COMMENT '마지막으로 읽은 메시지 ID (안읽음 수 계산용)',
  `joined_at`            DATETIME NOT NULL COMMENT '참여 일시',
  PRIMARY KEY (`chat_participant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='채팅 참여자';

CREATE TABLE `chat_messages` (
  `message_id`   BIGINT      NOT NULL AUTO_INCREMENT COMMENT '메시지 ID',
  `chat_room_id` BIGINT      NOT NULL COMMENT '채팅방',
  `sender_id`    BIGINT      NULL     COMMENT '보낸 회원 (SYSTEM 메시지면 NULL)',
  `message_type` VARCHAR(20) NOT NULL DEFAULT 'TEXT' COMMENT 'TEXT | IMAGE | SYSTEM(나눔 연결·나눔중 변경·수령 확인 안내) | APPOINTMENT(전달 약속 카드: matches 일정·장소·방법 참조)',
  `content`      TEXT        NOT NULL COMMENT '메시지 내용 또는 이미지 URL',
  `created_at`   DATETIME    NOT NULL COMMENT '전송 일시',
  PRIMARY KEY (`message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='채팅 메시지';

-- ---------- 알림 ----------
-- GET /api/notifications, /unread-count, PATCH /{id}/read, /read-all, SUB /user/queue/notifications
CREATE TABLE `notifications` (
  `notification_id` BIGINT       NOT NULL AUTO_INCREMENT COMMENT '알림 ID',
  `receiver_id`     BIGINT       NOT NULL COMMENT '수신 회원',
  `type`            VARCHAR(30)  NOT NULL COMMENT 'MATCH_REQUESTED(새 제안·요청) | MATCH_RESERVED(수락) | MATCH_REJECTED | MATCH_SCHEDULED(전달 일정 확정) | MATCH_SHARING(전달 준비 시작) | MATCH_RECEIVE_REQUIRED(수령 확인 요청) | MATCH_COMPLETED | MATCH_CANCELED(취소·수량 복원) | CHAT',
  `title`           VARCHAR(100) NOT NULL COMMENT '알림 제목',
  `content`         VARCHAR(255) NULL     COMMENT '알림 본문',
  `target_type`     VARCHAR(20)  NULL     COMMENT '이동 대상: MATCH | CHAT_ROOM | DONATION_ITEM | NEEDED_ITEM',
  `target_id`       BIGINT       NULL     COMMENT '이동 대상 ID',
  `is_read`         BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '읽음 여부',
  `read_at`         DATETIME     NULL     COMMENT '읽은 일시',
  `created_at`      DATETIME     NOT NULL COMMENT '생성 일시',
  PRIMARY KEY (`notification_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='알림';

-- [v3 추가] GET/PATCH /api/users/me/notification-settings
-- 회원가입 시 기본값(전부 ON)으로 1행 생성. ※ Figma에 알림 설정 화면이 아직 없어 항목은 임시
CREATE TABLE `notification_settings` (
  `user_id`       BIGINT   NOT NULL COMMENT '회원 ID (users PK 공유)',
  `match_enabled` BOOLEAN  NOT NULL DEFAULT TRUE COMMENT '매칭 알림 (제안·요청, 수락·거절, 나눔중, 완료, 취소)',
  `chat_enabled`  BOOLEAN  NOT NULL DEFAULT TRUE COMMENT '채팅 알림',
  `created_at`    DATETIME NOT NULL COMMENT '생성 일시',
  `updated_at`    DATETIME NOT NULL COMMENT '수정 일시',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='알림 설정';

-- [v3 추가] POST /api/users/me/push-tokens, DELETE /api/users/me/push-tokens/{pushTokenId} (후순위)
-- 기기별 1행. 같은 토큰이 다른 계정으로 다시 등록되면 user_id 갱신
CREATE TABLE `push_tokens` (
  `push_token_id` BIGINT       NOT NULL AUTO_INCREMENT COMMENT '푸시 토큰 ID',
  `user_id`       BIGINT       NOT NULL COMMENT '회원 ID',
  `token`         VARCHAR(255) NOT NULL COMMENT 'FCM 등록 토큰',
  `device_type`   VARCHAR(20)  NOT NULL COMMENT 'ANDROID | IOS | WEB',
  `created_at`    DATETIME     NOT NULL COMMENT '등록 일시',
  `updated_at`    DATETIME     NOT NULL COMMENT '갱신 일시 (마지막 등록 요청)',
  PRIMARY KEY (`push_token_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='푸시 토큰';

-- [v3 추가] Figma A13 비밀번호 재설정(이메일 입력) → 메일 안내 → A14 새 비밀번호
-- 메일 링크의 토큰은 원문 대신 SHA-256 해시만 저장, 1회 사용 후 used_at 기록
CREATE TABLE `password_reset_tokens` (
  `reset_token_id` BIGINT   NOT NULL AUTO_INCREMENT COMMENT '재설정 토큰 ID',
  `user_id`        BIGINT   NOT NULL COMMENT '회원 ID',
  `token_hash`     CHAR(64) NOT NULL COMMENT '재설정 토큰 SHA-256 해시',
  `expires_at`     DATETIME NOT NULL COMMENT '만료 일시 (발급 후 30분 등)',
  `used_at`        DATETIME NULL     COMMENT '사용 일시 (NULL = 미사용)',
  `created_at`     DATETIME NOT NULL COMMENT '발급 일시',
  PRIMARY KEY (`reset_token_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='비밀번호 재설정 토큰';


-- =====================================================================
-- [2] FOREIGN KEYS
-- =====================================================================

ALTER TABLE `terms_agreements` ADD CONSTRAINT `FK_users_TO_terms_agreements` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);
ALTER TABLE `donors`           ADD CONSTRAINT `FK_users_TO_donors`           FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);
ALTER TABLE `organizations`    ADD CONSTRAINT `FK_users_TO_organizations`    FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);
ALTER TABLE `donors`           ADD CONSTRAINT `FK_regions_TO_donors`         FOREIGN KEY (`region_id`) REFERENCES `regions` (`region_id`);

ALTER TABLE `organization_verifications` ADD CONSTRAINT `FK_organizations_TO_organization_verifications` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`user_id`);

ALTER TABLE `donation_items` ADD CONSTRAINT `FK_donors_TO_donation_items`          FOREIGN KEY (`donor_id`)    REFERENCES `donors` (`user_id`);
ALTER TABLE `donation_items` ADD CONSTRAINT `FK_item_categories_TO_donation_items` FOREIGN KEY (`category_id`) REFERENCES `item_categories` (`category_id`);
ALTER TABLE `donation_items` ADD CONSTRAINT `FK_regions_TO_donation_items`         FOREIGN KEY (`region_id`)   REFERENCES `regions` (`region_id`);

ALTER TABLE `donation_item_images` ADD CONSTRAINT `FK_donation_items_TO_donation_item_images` FOREIGN KEY (`donation_item_id`) REFERENCES `donation_items` (`donation_item_id`);

ALTER TABLE `needed_items` ADD CONSTRAINT `FK_organizations_TO_needed_items`   FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`user_id`);
ALTER TABLE `needed_items` ADD CONSTRAINT `FK_item_categories_TO_needed_items` FOREIGN KEY (`category_id`)     REFERENCES `item_categories` (`category_id`);
ALTER TABLE `needed_items` ADD CONSTRAINT `FK_regions_TO_needed_items`         FOREIGN KEY (`region_id`)       REFERENCES `regions` (`region_id`);

ALTER TABLE `organization_recommendations` ADD CONSTRAINT `FK_donation_items_TO_organization_recommendations` FOREIGN KEY (`donation_item_id`) REFERENCES `donation_items` (`donation_item_id`);
ALTER TABLE `organization_recommendations` ADD CONSTRAINT `FK_needed_items_TO_organization_recommendations`   FOREIGN KEY (`needed_item_id`)   REFERENCES `needed_items` (`needed_item_id`);
ALTER TABLE `organization_recommendations` ADD CONSTRAINT `FK_organizations_TO_organization_recommendations`  FOREIGN KEY (`organization_id`)  REFERENCES `organizations` (`user_id`);

ALTER TABLE `donation_item_recommendations` ADD CONSTRAINT `FK_needed_items_TO_donation_item_recommendations`   FOREIGN KEY (`needed_item_id`)   REFERENCES `needed_items` (`needed_item_id`);
ALTER TABLE `donation_item_recommendations` ADD CONSTRAINT `FK_donation_items_TO_donation_item_recommendations` FOREIGN KEY (`donation_item_id`) REFERENCES `donation_items` (`donation_item_id`);

ALTER TABLE `matches` ADD CONSTRAINT `FK_donation_items_TO_matches` FOREIGN KEY (`donation_item_id`) REFERENCES `donation_items` (`donation_item_id`);
ALTER TABLE `matches` ADD CONSTRAINT `FK_needed_items_TO_matches`   FOREIGN KEY (`needed_item_id`)   REFERENCES `needed_items` (`needed_item_id`);
ALTER TABLE `matches` ADD CONSTRAINT `FK_donors_TO_matches`         FOREIGN KEY (`donor_id`)         REFERENCES `donors` (`user_id`);
ALTER TABLE `matches` ADD CONSTRAINT `FK_organizations_TO_matches`  FOREIGN KEY (`organization_id`)  REFERENCES `organizations` (`user_id`);
ALTER TABLE `matches` ADD CONSTRAINT `FK_users_TO_matches`          FOREIGN KEY (`canceled_by`)      REFERENCES `users` (`user_id`);

ALTER TABLE `chat_rooms`        ADD CONSTRAINT `FK_matches_TO_chat_rooms`           FOREIGN KEY (`match_id`)     REFERENCES `matches` (`match_id`);
ALTER TABLE `chat_participants` ADD CONSTRAINT `FK_chat_rooms_TO_chat_participants` FOREIGN KEY (`chat_room_id`) REFERENCES `chat_rooms` (`chat_room_id`);
ALTER TABLE `chat_participants` ADD CONSTRAINT `FK_users_TO_chat_participants`      FOREIGN KEY (`user_id`)      REFERENCES `users` (`user_id`);
ALTER TABLE `chat_messages`     ADD CONSTRAINT `FK_chat_rooms_TO_chat_messages`     FOREIGN KEY (`chat_room_id`) REFERENCES `chat_rooms` (`chat_room_id`);
ALTER TABLE `chat_messages`     ADD CONSTRAINT `FK_users_TO_chat_messages`          FOREIGN KEY (`sender_id`)    REFERENCES `users` (`user_id`);

ALTER TABLE `notifications`         ADD CONSTRAINT `FK_users_TO_notifications`         FOREIGN KEY (`receiver_id`) REFERENCES `users` (`user_id`);
ALTER TABLE `notification_settings` ADD CONSTRAINT `FK_users_TO_notification_settings` FOREIGN KEY (`user_id`)     REFERENCES `users` (`user_id`);
ALTER TABLE `push_tokens`           ADD CONSTRAINT `FK_users_TO_push_tokens`           FOREIGN KEY (`user_id`)     REFERENCES `users` (`user_id`);
ALTER TABLE `password_reset_tokens` ADD CONSTRAINT `FK_users_TO_password_reset_tokens` FOREIGN KEY (`user_id`)     REFERENCES `users` (`user_id`);


-- =====================================================================
-- [3] UNIQUE / INDEX  (실제 DB용 — ERDCloud 가져오기 에러 시 이 블록 제외)
--     FK 컬럼 단일 인덱스는 InnoDB가 자동 생성하므로 복합 인덱스만 추가
-- =====================================================================

ALTER TABLE `users`             ADD CONSTRAINT `UQ_users_email`                   UNIQUE (`email`);
ALTER TABLE `organizations`     ADD CONSTRAINT `UQ_organizations_business_number` UNIQUE (`business_number`);
ALTER TABLE `regions`           ADD CONSTRAINT `UQ_regions_sido_sigungu`          UNIQUE (`sido`, `sigungu`);
ALTER TABLE `chat_rooms`        ADD CONSTRAINT `UQ_chat_rooms_match`              UNIQUE (`match_id`);
ALTER TABLE `chat_participants` ADD CONSTRAINT `UQ_chat_participants_room_user`   UNIQUE (`chat_room_id`, `user_id`);
ALTER TABLE `organization_recommendations`  ADD CONSTRAINT `UQ_org_rec_item_needed`  UNIQUE (`donation_item_id`, `needed_item_id`);
ALTER TABLE `donation_item_recommendations` ADD CONSTRAINT `UQ_item_rec_needed_item` UNIQUE (`needed_item_id`, `donation_item_id`);
ALTER TABLE `push_tokens`       ADD CONSTRAINT `UQ_push_tokens_token`             UNIQUE (`token`);
ALTER TABLE `password_reset_tokens` ADD CONSTRAINT `UQ_password_reset_tokens_hash` UNIQUE (`token_hash`);

CREATE INDEX `IDX_donation_items_status_created` ON `donation_items` (`status`, `created_at`);
CREATE INDEX `IDX_needed_items_status_created`   ON `needed_items` (`status`, `created_at`);
CREATE INDEX `IDX_needed_items_region_status`    ON `needed_items` (`region_id`, `status`);
CREATE INDEX `IDX_donation_items_region_status`  ON `donation_items` (`region_id`, `status`);
CREATE INDEX `IDX_matches_donor_status`          ON `matches` (`donor_id`, `status`);
CREATE INDEX `IDX_matches_org_status`            ON `matches` (`organization_id`, `status`);
CREATE INDEX `IDX_chat_messages_room_message`    ON `chat_messages` (`chat_room_id`, `message_id`);
CREATE INDEX `IDX_notifications_receiver_read`   ON `notifications` (`receiver_id`, `is_read`);
