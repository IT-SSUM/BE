package com.itssum.global.common;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 컨트롤러가 돌려준 ApiResponse 의 status(SuccessCode 의 HTTP 상태)로 응답 상태를 맞춘다. 예: 회원가입 201.
 * ResponseEntity 로 상태를 직접 정하는 응답(예외 처리기)은 대상이 아니다.
 */
@RestControllerAdvice
public class ApiResponseStatusAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return ApiResponse.class.isAssignableFrom(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof ApiResponse<?> apiResponse && apiResponse.status() != null) {
            response.setStatusCode(apiResponse.status());
        }
        return body;
    }
}
