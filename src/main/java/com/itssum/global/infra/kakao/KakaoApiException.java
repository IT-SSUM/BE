package com.itssum.global.infra.kakao;

/** 카카오 API 를 호출하지 못했을 때. 어떻게 처리할지는 호출한 도메인이 정한다. */
public class KakaoApiException extends RuntimeException {

    public KakaoApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
