package com.itssum.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 성공 코드. ErrorCode 와 같은 형식이며, 도메인 성공 코드는 해당 도메인 API 작업 때 섹션을 나눠 추가한다.
 * status 는 ApiResponseStatusAdvice 가 HTTP 응답 상태로 쓴다.
 */
@Getter
@RequiredArgsConstructor
public enum SuccessCode {

    // ---------- 공통 ----------
    OK(HttpStatus.OK, "COMMON_200", "요청에 성공했습니다."),
    CREATED(HttpStatus.CREATED, "COMMON_201", "요청한 리소스를 생성했습니다."),

    // ---------- 인증 ----------
    AUTH_EMAIL_VERIFICATION_SENT(HttpStatus.OK, "AUTH_200", "인증번호를 보냈습니다."),
    AUTH_EMAIL_VERIFIED(HttpStatus.OK, "AUTH_200_1", "이메일 인증이 완료되었습니다."),
    AUTH_LOGIN(HttpStatus.OK, "AUTH_200_2", "로그인에 성공했습니다."),
    AUTH_TOKEN_REFRESHED(HttpStatus.OK, "AUTH_200_3", "토큰을 재발급했습니다."),
    AUTH_LOGOUT(HttpStatus.OK, "AUTH_200_4", "로그아웃했습니다."),
    AUTH_BUSINESS_VERIFIED(HttpStatus.OK, "AUTH_200_5", "사업자 확인이 완료되었습니다."),
    AUTH_SIGN_UP(HttpStatus.CREATED, "AUTH_201", "회원가입이 완료되었습니다."),

    // ---------- 회원 ----------
    USER_DONOR_PROFILE_FOUND(HttpStatus.OK, "USER_200", "기부자 프로필을 조회했습니다."),
    USER_DONOR_PROFILE_UPDATED(HttpStatus.OK, "USER_200_1", "기부자 프로필을 수정했습니다."),
    USER_ORGANIZATION_PROFILE_FOUND(HttpStatus.OK, "USER_200_2", "기관 정보를 조회했습니다."),
    USER_ORGANIZATION_PROFILE_UPDATED(HttpStatus.OK, "USER_200_3", "기관 정보를 수정했습니다."),
    USER_ORGANIZATION_DETAIL_FOUND(HttpStatus.OK, "USER_200_4", "기관 상세 정보를 조회했습니다."),
    USER_WITHDRAWN(HttpStatus.OK, "USER_200_5", "회원 탈퇴가 완료되었습니다."),

    // ---------- 물품 ----------
    ITEM_CONDITIONS_FOUND(HttpStatus.OK, "ITEM_200", "물품 상태 기준을 조회했습니다."),
    ITEM_CATEGORIES_FOUND(HttpStatus.OK, "ITEM_200_1", "물품 카테고리 목록을 조회했습니다."),
    ITEM_UNITS_FOUND(HttpStatus.OK, "ITEM_200_2", "물품 단위 목록을 조회했습니다."),
    ITEM_IMAGE_UPLOAD_URL_ISSUED(HttpStatus.OK, "ITEM_200_3", "물품 사진 업로드 URL을 발급했습니다."),
    ITEM_DONATION_LIST_FOUND(HttpStatus.OK, "ITEM_200_4", "기부 물품 목록을 조회했습니다."),
    ITEM_DONATION_FOUND(HttpStatus.OK, "ITEM_200_5", "기부 물품을 조회했습니다."),
    ITEM_DONATION_UPDATED(HttpStatus.OK, "ITEM_200_6", "기부 물품을 수정했습니다."),
    ITEM_DONATION_DELETED(HttpStatus.OK, "ITEM_200_7", "기부 물품을 삭제했습니다."),
    ITEM_MY_DONATIONS_FOUND(HttpStatus.OK, "ITEM_200_8", "내가 등록한 기부 물품 목록을 조회했습니다."),
    ITEM_NEEDED_LIST_FOUND(HttpStatus.OK, "ITEM_200_9", "필요 물품 목록을 조회했습니다."),
    ITEM_NEEDED_FOUND(HttpStatus.OK, "ITEM_200_10", "필요 물품을 조회했습니다."),
    ITEM_NEEDED_UPDATED(HttpStatus.OK, "ITEM_200_11", "필요 물품을 수정했습니다."),
    ITEM_NEEDED_DELETED(HttpStatus.OK, "ITEM_200_12", "필요 물품을 삭제했습니다."),
    ITEM_MY_NEEDED_FOUND(HttpStatus.OK, "ITEM_200_13", "우리 기관 필요 물품 목록을 조회했습니다."),
    ITEM_DONATION_CREATED(HttpStatus.CREATED, "ITEM_201", "기부 물품을 등록했습니다."),
    ITEM_NEEDED_CREATED(HttpStatus.CREATED, "ITEM_201_1", "필요 물품을 등록했습니다."),

    // ---------- 매칭 ----------
    MATCH_LIST_FOUND(HttpStatus.OK, "MATCH_200", "매칭 목록을 조회했습니다."),
    MATCH_FOUND(HttpStatus.OK, "MATCH_200_1", "매칭을 조회했습니다."),
    MATCH_RESERVED(HttpStatus.OK, "MATCH_200_2", "거래를 예약했습니다."),
    MATCH_COMPLETED(HttpStatus.OK, "MATCH_200_3", "전달 완료 처리했습니다."),
    MATCH_CANCELED(HttpStatus.OK, "MATCH_200_4", "거래를 취소했습니다."),
    MATCH_APPOINTMENT_SAVED(HttpStatus.OK, "MATCH_200_5", "전달 약속을 저장했습니다."),
    MATCH_ACCEPTED(HttpStatus.OK, "MATCH_200_6", "기부 제안·요청을 수락했습니다."),
    MATCH_REJECTED(HttpStatus.OK, "MATCH_200_7", "기부 제안·요청을 거절했습니다."),
    MATCH_WITHDRAWN(HttpStatus.OK, "MATCH_200_8", "기부 제안·요청을 철회했습니다."),
    MATCH_QUANTITY_UPDATED(HttpStatus.OK, "MATCH_200_9", "거래중 합의 수량을 변경했습니다."),
    MATCH_CREATED(HttpStatus.CREATED, "MATCH_201", "기부 제안·요청을 생성했습니다."),

    // ---------- 채팅 ----------
    CHAT_ROOMS_FOUND(HttpStatus.OK, "CHAT_200", "채팅방 목록을 조회했습니다."),
    CHAT_MESSAGES_FOUND(HttpStatus.OK, "CHAT_200_1", "채팅 메시지 내역을 조회했습니다."),
    CHAT_READ_MARKED(HttpStatus.OK, "CHAT_200_2", "채팅 메시지를 읽음 처리했습니다."),
    CHAT_IMAGE_UPLOAD_URL_ISSUED(HttpStatus.OK, "CHAT_200_3", "이미지 업로드 URL을 발급했습니다."),

    // ---------- 알림 ----------
    NOTIFICATION_LIST_FOUND(HttpStatus.OK, "NOTIFICATION_200", "알림 목록을 조회했습니다."),
    NOTIFICATION_UNREAD_COUNT_FOUND(HttpStatus.OK, "NOTIFICATION_200_1", "안 읽은 알림 수를 조회했습니다."),
    NOTIFICATION_READ(HttpStatus.OK, "NOTIFICATION_200_2", "알림을 읽음 처리했습니다."),
    NOTIFICATION_ALL_READ(HttpStatus.OK, "NOTIFICATION_200_3", "알림을 모두 읽음 처리했습니다."),
    NOTIFICATION_SETTINGS_FOUND(HttpStatus.OK, "NOTIFICATION_200_4", "알림 설정을 조회했습니다."),
    NOTIFICATION_SETTINGS_UPDATED(HttpStatus.OK, "NOTIFICATION_200_5", "알림 설정을 수정했습니다."),
    PUSH_REGISTRATION_DELETED(HttpStatus.OK, "NOTIFICATION_200_6", "푸시 등록을 삭제했습니다."),
    PUSH_REGISTRATION_REGISTERED(HttpStatus.CREATED, "NOTIFICATION_201", "기기 푸시를 등록했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
