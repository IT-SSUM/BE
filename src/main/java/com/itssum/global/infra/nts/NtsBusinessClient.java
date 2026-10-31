package com.itssum.global.infra.nts;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 국세청 사업자등록정보 진위확인 API 호출. 진위확인 응답에 상태(계속·휴업·폐업)도 들어 있어서 상태조회는 따로 부르지 않는다.
 */
@Component
public class NtsBusinessClient {

    private static final DateTimeFormatter NTS_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final RestClient restClient;
    private final String serviceKey;

    @Autowired
    public NtsBusinessClient(NtsProperties properties) {
        this(RestClient.builder().requestFactory(requestFactory(properties)), properties);
    }

    NtsBusinessClient(RestClient.Builder builder, NtsProperties properties) {
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
        this.serviceKey = properties.serviceKey();
    }

    /** 대표자명(p_nm)까지 보내고, 상호(b_nm)는 한 글자만 달라도 불일치가 나서 보내지 않는다. */
    public NtsValidation validate(String businessNumber, LocalDate openingDate, String representativeName) {
        ValidateRequest request = new ValidateRequest(
                List.of(new Business(businessNumber, openingDate.format(NTS_DATE), representativeName)));
        ValidateResponse response;
        try {
            // 서비스키에 + / = 가 들어 있어서 URI 변수로 넘겨 인코딩한다
            response = restClient.post()
                    .uri("/validate?serviceKey={serviceKey}", serviceKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ValidateResponse.class);
        } catch (RestClientException e) {
            throw new NtsApiException("국세청 진위확인 API 호출 실패", e);
        }
        if (response == null || !"OK".equals(response.statusCode())
                || response.data() == null || response.data().isEmpty()) {
            throw new NtsApiException("국세청 진위확인 API 응답을 해석할 수 없음: "
                    + (response == null ? null : response.statusCode()));
        }
        Result result = response.data().getFirst();
        Status status = result.status();
        if (status == null) {
            return new NtsValidation(result.valid(), null, null, null);
        }
        return new NtsValidation(result.valid(), status.businessStatusCode(), status.taxType(),
                parseDate(status.closedDate()));
    }

    private static LocalDate parseDate(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value, NTS_DATE);
    }

    private static SimpleClientHttpRequestFactory requestFactory(NtsProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.timeout());
        factory.setReadTimeout(properties.timeout());
        return factory;
    }

    record ValidateRequest(List<Business> businesses) {
    }

    record Business(
            @JsonProperty("b_no") String businessNumber,
            @JsonProperty("start_dt") String openingDate,
            @JsonProperty("p_nm") String representativeName) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ValidateResponse(@JsonProperty("status_code") String statusCode, List<Result> data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Result(String valid, Status status) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Status(
            @JsonProperty("b_stt_cd") String businessStatusCode,
            @JsonProperty("tax_type") String taxType,
            @JsonProperty("end_dt") String closedDate) {
    }
}
