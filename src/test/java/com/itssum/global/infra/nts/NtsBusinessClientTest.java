package com.itssum.global.infra.nts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class NtsBusinessClientTest {

    private static final LocalDate OPENING_DATE = LocalDate.of(2015, 3, 2);

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final NtsBusinessClient client = new NtsBusinessClient(builder,
            new NtsProperties("https://nts.test/v1", "key+/=", Duration.ofSeconds(5)));

    @Test
    void validateSendsBusinessInfoWithEncodedKeyAndReadsStatus() {
        server.expect(requestTo("https://nts.test/v1/validate?serviceKey=key%2B%2F%3D"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"businesses": [{"b_no": "1234567890", "start_dt": "20150302", "p_nm": "김수현"}]}
                        """, JsonCompareMode.STRICT))
                .andRespond(withSuccess("""
                        {"status_code": "OK", "request_cnt": 1, "valid_cnt": 1,
                         "data": [{"b_no": "1234567890", "valid": "01", "valid_msg": "",
                                   "status": {"b_no": "1234567890", "b_stt": "계속사업자", "b_stt_cd": "01",
                                              "tax_type": "부가가치세 일반과세자", "tax_type_cd": "01",
                                              "end_dt": "", "utcc_yn": "N"}}]}
                        """, MediaType.APPLICATION_JSON));

        NtsValidation result = client.validate("1234567890", OPENING_DATE, "김수현");

        server.verify();
        assertEquals(new NtsValidation("01", "01", "부가가치세 일반과세자", null), result);
        assertTrue(result.matched());
        assertFalse(result.suspendedOrClosed());
    }

    @Test
    void mismatchHasNoStatus() {
        respond("""
                {"status_code": "OK", "request_cnt": 1, "valid_cnt": 0,
                 "data": [{"b_no": "1234567890", "valid": "02", "valid_msg": "확인할 수 없습니다."}]}
                """);

        NtsValidation result = client.validate("1234567890", OPENING_DATE, "김수현");

        assertEquals(new NtsValidation("02", null, null, null), result);
        assertFalse(result.matched());
    }

    @Test
    void closedBusinessHasClosedDate() {
        respond("""
                {"status_code": "OK", "data": [{"valid": "01",
                 "status": {"b_stt_cd": "03", "tax_type": "부가가치세 간이과세자", "end_dt": "20240131"}}]}
                """);

        NtsValidation result = client.validate("1234567890", OPENING_DATE, "김수현");

        assertEquals(LocalDate.of(2024, 1, 31), result.closedDate());
        assertTrue(result.suspendedOrClosed());
    }

    @Test
    void malformedClosedDateIsNtsApiException() {
        respond("""
                {"status_code": "OK", "data": [{"valid": "01",
                 "status": {"b_stt_cd": "03", "tax_type": "부가가치세 간이과세자", "end_dt": "2024-01-31"}}]}
                """);

        assertThrows(NtsApiException.class, () -> client.validate("1234567890", OPENING_DATE, "김수현"));
    }

    @Test
    void httpErrorIsNtsApiException() {
        server.expect(requestTo("https://nts.test/v1/validate?serviceKey=key%2B%2F%3D"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(NtsApiException.class, () -> client.validate("1234567890", OPENING_DATE, "김수현"));
    }

    @Test
    void responseWithoutResultIsNtsApiException() {
        respond("""
                {"status_code": "BAD_JSON_REQUEST", "data": []}
                """);

        assertThrows(NtsApiException.class, () -> client.validate("1234567890", OPENING_DATE, "김수현"));
    }

    private void respond(String body) {
        server.expect(requestTo("https://nts.test/v1/validate?serviceKey=key%2B%2F%3D"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }
}
