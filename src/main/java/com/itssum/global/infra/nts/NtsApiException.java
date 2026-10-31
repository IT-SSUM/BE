package com.itssum.global.infra.nts;

/** 국세청 API 를 호출하지 못했거나 응답을 해석할 수 없을 때. 어떤 에러 코드로 응답할지는 호출한 도메인이 정한다. */
public class NtsApiException extends RuntimeException {

    public NtsApiException(String message) {
        super(message);
    }

    public NtsApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
