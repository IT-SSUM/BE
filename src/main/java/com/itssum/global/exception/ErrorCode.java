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

    // ---------- 채팅 ----------
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_404", "채팅방을 찾을 수 없습니다."),
    CHAT_NOT_PARTICIPANT(HttpStatus.FORBIDDEN, "CHAT_403", "채팅방 참여자만 접근할 수 있습니다."),
    CHAT_READ_ONLY(HttpStatus.CONFLICT, "CHAT_409", "종료된 거래의 채팅방에는 메시지를 보낼 수 없습니다."),
    CHAT_IMAGE_NOT_UPLOADED(HttpStatus.BAD_REQUEST, "CHAT_400", "업로드된 채팅 이미지를 확인할 수 없습니다."),
    CHAT_IMAGE_STORAGE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "CHAT_503", "이미지 저장소가 준비되지 않았습니다."),
    CHAT_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_404_1", "채팅 메시지를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
