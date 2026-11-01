package com.itssum.global.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.itssum.global.common.ApiResponse.FieldErrorBody;
import com.itssum.global.exception.ErrorCode;
import com.itssum.global.exception.SuccessCode;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;

class ApiResponseTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void successCarriesSuccessCodeAndDataWithoutExposingStatus() {
        ApiResponse<Map<String, Integer>> response = ApiResponse.ok(SuccessCode.AUTH_SIGN_UP, Map.of("userId", 1));

        Map<String, Object> body = toMap(response);

        assertEquals(Set.of("success", "code", "message", "data"), body.keySet());
        assertEquals(true, body.get("success"));
        assertEquals("AUTH_201", body.get("code"));
        assertEquals("회원가입이 완료되었습니다.", body.get("message"));
        assertEquals(Map.of("userId", 1), body.get("data"));
        assertEquals(HttpStatus.CREATED, response.status());
    }

    @Test
    void successWithoutDataOmitsData() {
        Map<String, Object> body = toMap(ApiResponse.ok(SuccessCode.AUTH_LOGOUT));

        assertEquals(Set.of("success", "code", "message"), body.keySet());
        assertEquals("AUTH_200_4", body.get("code"));
    }

    @Test
    void defaultSuccessIsCommon200() {
        ApiResponse<Void> response = ApiResponse.ok();

        assertEquals("COMMON_200", response.code());
        assertEquals(HttpStatus.OK, response.status());
    }

    @Test
    void failureUsesErrorCodeAtSameLevel() {
        ApiResponse<Void> response = ApiResponse.fail(ErrorCode.AUTH_INVALID_CREDENTIALS);

        Map<String, Object> body = toMap(response);

        assertEquals(Set.of("success", "code", "message"), body.keySet());
        assertEquals(false, body.get("success"));
        assertEquals("AUTH_401", body.get("code"));
        assertEquals(HttpStatus.UNAUTHORIZED, response.status());
    }

    @Test
    void validationFailureCarriesFieldErrors() {
        Map<String, Object> body = toMap(ApiResponse.fail(ErrorCode.INVALID_INPUT,
                List.of(new FieldErrorBody("password", "8자 이상"))));

        assertEquals("COMMON_400", body.get("code"));
        assertEquals(List.of(Map.of("field", "password", "reason", "8자 이상")), body.get("fieldErrors"));
        assertFalse(body.containsKey("data"));
    }

    private Map<String, Object> toMap(ApiResponse<?> response) {
        return JsonPath.read(jsonMapper.writeValueAsString(response), "$");
    }
}
