package com.itssum.global.infra.kakao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KakaoLocalClientTest {

    private static final String ADDRESS = "서울 구로구 가마산로 12";

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final KakaoLocalClient client = new KakaoLocalClient(builder,
            new KakaoProperties("https://kakao.test", "rest-key", Duration.ofSeconds(3)));

    @Test
    void geocodeSendsAddressWithKeyAndRoundsToSevenDecimals() {
        server.expect(requestTo(Matchers.startsWith("https://kakao.test/v2/local/search/address.json?query=")))
                .andExpect(request -> assertEquals("query=" + ADDRESS, request.getURI().getQuery()))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "KakaoAK rest-key"))
                .andRespond(withSuccess("""
                        {"meta": {"total_count": 1, "pageable_count": 1, "is_end": true},
                         "documents": [{"address_name": "서울 구로구 가마산로 12", "address_type": "ROAD_ADDR",
                                        "x": "126.886829423468", "y": "37.4930157637402",
                                        "address": {}, "road_address": {}}]}
                        """, MediaType.APPLICATION_JSON));

        Optional<GeoPoint> result = client.geocode(ADDRESS);

        server.verify();
        assertEquals(Optional.of(new GeoPoint(new BigDecimal("37.4930158"), new BigDecimal("126.8868294"))), result);
    }

    @Test
    void unknownAddressIsEmpty() {
        server.expect(requestTo(Matchers.startsWith("https://kakao.test/v2/local/search/address.json")))
                .andRespond(withSuccess("""
                        {"meta": {"total_count": 0, "pageable_count": 0, "is_end": true}, "documents": []}
                        """, MediaType.APPLICATION_JSON));

        assertTrue(client.geocode("없는 주소").isEmpty());
    }

    @Test
    void missingOrNonNumericCoordinateIsKakaoApiException() {
        respondDocument("""
                {"x": "126.886829423468"}
                """);
        respondDocument("""
                {"x": "abc", "y": "37.4930157637402"}
                """);

        assertThrows(KakaoApiException.class, () -> client.geocode(ADDRESS));
        assertThrows(KakaoApiException.class, () -> client.geocode(ADDRESS));
    }

    @Test
    void httpErrorIsKakaoApiException() {
        server.expect(requestTo(Matchers.startsWith("https://kakao.test/v2/local/search/address.json")))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(KakaoApiException.class, () -> client.geocode(ADDRESS));
    }

    private void respondDocument(String document) {
        server.expect(requestTo(Matchers.startsWith("https://kakao.test/v2/local/search/address.json")))
                .andRespond(withSuccess("""
                        {"meta": {"total_count": 1}, "documents": [%s]}
                        """.formatted(document), MediaType.APPLICATION_JSON));
    }
}
