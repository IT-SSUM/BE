package com.itssum.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드. 도메인 에러는 해당 도메인 API 작업 때 섹션을 나눠 추가한다 (예: USER_xxx, MATCH_xxx).
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ---------- 공통 ----------
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_400", "입력값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON_401", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON_403", "접근 권한이 없습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_404", "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON_405", "지원하지 않는 HTTP 메서드입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_500", "서버 내부 오류가 발생했습니다."),

    // ---------- 인증 ----------
    AUTH_REQUIRED_TERMS_NOT_AGREED(HttpStatus.BAD_REQUEST, "AUTH_400", "필수 약관에 동의해야 합니다."),
    AUTH_VERIFICATION_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "AUTH_400_1", "인증번호가 일치하지 않습니다."),
    AUTH_VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "AUTH_400_2", "인증번호가 만료되었습니다. 다시 요청해 주세요."),
    AUTH_EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "AUTH_400_3", "이메일 인증이 필요합니다."),
    AUTH_BUSINESS_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "AUTH_400_4", "사업자 확인이 필요합니다."),
    AUTH_BUSINESS_INFO_MISMATCH(HttpStatus.BAD_REQUEST, "AUTH_400_5", "사업자 정보가 국세청 등록 정보와 일치하지 않습니다."),
    AUTH_BUSINESS_CLOSED(HttpStatus.BAD_REQUEST, "AUTH_400_6", "휴업 또는 폐업한 사업자는 가입할 수 없습니다."),
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_401", "이메일 또는 비밀번호가 올바르지 않습니다."),
    AUTH_INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_401_1", "다시 로그인해 주세요."),
    AUTH_EMAIL_DUPLICATED(HttpStatus.CONFLICT, "AUTH_409", "이미 가입된 이메일입니다."),
    AUTH_BUSINESS_NUMBER_DUPLICATED(HttpStatus.CONFLICT, "AUTH_409_1", "이미 가입된 사업자번호입니다."),
    AUTH_VERIFICATION_TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "AUTH_429", "잠시 후 다시 요청해 주세요."),
    AUTH_MAIL_SEND_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "AUTH_503", "인증 메일을 보내지 못했습니다. 잠시 후 다시 시도해 주세요."),
    AUTH_BUSINESS_VERIFICATION_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AUTH_503_1",
            "사업자 확인 서비스에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요."),

    // ---------- 회원 ----------
    USER_ORGANIZATION_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_404", "기관을 찾을 수 없습니다."),
    USER_WITHDRAWAL_BLOCKED(HttpStatus.CONFLICT, "USER_409",
            "진행 중인 거래가 있어 탈퇴할 수 없습니다. 응답 대기 중인 제안·요청과 거래중·예약중인 거래를 먼저 마무리해 주세요."),

    // ---------- 기준 데이터 ----------
    REGION_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "REGION_400", "현재는 경기 부천시만 선택할 수 있습니다."),
    REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "REGION_404", "지역을 찾을 수 없습니다."),

    // ---------- 채팅 ----------
    CHAT_IMAGE_NOT_UPLOADED(HttpStatus.BAD_REQUEST, "CHAT_400", "업로드된 채팅 이미지를 확인할 수 없습니다."),
    CHAT_NOT_PARTICIPANT(HttpStatus.FORBIDDEN, "CHAT_403", "채팅방 참여자만 접근할 수 있습니다."),
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_404", "채팅방을 찾을 수 없습니다."),
    CHAT_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_404_1", "채팅 메시지를 찾을 수 없습니다."),
    CHAT_READ_ONLY(HttpStatus.CONFLICT, "CHAT_409", "종료된 거래의 채팅방에는 메시지를 보낼 수 없습니다."),
    CHAT_IMAGE_STORAGE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "CHAT_503", "이미지 저장소가 준비되지 않았습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
