package com.itssum.global.infra.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 카카오 로컬 API '주소 검색'으로 도로명·지번 주소를 좌표로 바꾼다. 거리순 추천의 기준 좌표로 쓴다.
 */
@Component
public class KakaoLocalClient {

    private static final int COORDINATE_SCALE = 7;

    private final RestClient restClient;

    @Autowired
    public KakaoLocalClient(KakaoProperties properties) {
        this(RestClient.builder().requestFactory(requestFactory(properties)), properties);
    }

    KakaoLocalClient(RestClient.Builder builder, KakaoProperties properties) {
        this.restClient = builder.baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + properties.restApiKey())
                .build();
    }

    /** 검색 결과가 없으면 빈 값. 여러 건이면 카카오가 정확도 순으로 주는 첫 번째를 쓴다. */
    public Optional<GeoPoint> geocode(String address) {
        AddressSearchResponse response;
        try {
            response = restClient.get()
                    .uri("/v2/local/search/address.json?query={query}", address)
                    .retrieve()
                    .body(AddressSearchResponse.class);
        } catch (RestClientException e) {
            throw new KakaoApiException("카카오 주소 검색 API 호출 실패", e);
        }
        if (response == null || response.documents() == null || response.documents().isEmpty()) {
            return Optional.empty();
        }
        // x 가 경도, y 가 위도다
        Document document = response.documents().getFirst();
        return Optional.of(new GeoPoint(coordinate(document.y()), coordinate(document.x())));
    }

    private static BigDecimal coordinate(String value) {
        return new BigDecimal(value).setScale(COORDINATE_SCALE, RoundingMode.HALF_UP);
    }

    private static SimpleClientHttpRequestFactory requestFactory(KakaoProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.timeout());
        factory.setReadTimeout(properties.timeout());
        return factory;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AddressSearchResponse(List<Document> documents) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Document(String x, String y) {
    }
}
