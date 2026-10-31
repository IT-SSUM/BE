package com.itssum.global.common;

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

    // ---------- 물품 ----------
    ITEM_CONDITIONS_FOUND(HttpStatus.OK, "ITEM_200", "물품 상태 기준을 조회했습니다."),

    // ---------- 채팅 ----------
    CHAT_ROOMS_FOUND(HttpStatus.OK, "CHAT_200", "채팅방 목록을 조회했습니다."),
    CHAT_MESSAGES_FOUND(HttpStatus.OK, "CHAT_200_1", "채팅 메시지 내역을 조회했습니다."),
    CHAT_READ_MARKED(HttpStatus.OK, "CHAT_200_2", "채팅 메시지를 읽음 처리했습니다."),
    CHAT_IMAGE_UPLOAD_URL_ISSUED(HttpStatus.OK, "CHAT_200_3", "이미지 업로드 URL을 발급했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
