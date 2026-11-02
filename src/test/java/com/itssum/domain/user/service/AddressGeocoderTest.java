package com.itssum.domain.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.itssum.global.infra.kakao.GeoPoint;
import com.itssum.global.infra.kakao.KakaoApiException;
import com.itssum.global.infra.kakao.KakaoLocalClient;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.web.client.ResourceAccessException;

@ExtendWith(OutputCaptureExtension.class)
class AddressGeocoderTest {

    private static final String ADDRESS = "서울 구로구 가마산로 12";
    private static final GeoPoint POINT = new GeoPoint(new BigDecimal("37.4930158"), new BigDecimal("126.8868294"));

    private final KakaoLocalClient kakaoLocalClient = mock(KakaoLocalClient.class);
    private final AddressGeocoder geocoder = new AddressGeocoder(kakaoLocalClient);

    @Test
    void returnsPointFromKakao() {
        when(kakaoLocalClient.geocode(ADDRESS)).thenReturn(Optional.of(POINT));

        assertEquals(Optional.of(POINT), geocoder.locate(ADDRESS));
    }

    @Test
    void emptyWhenKakaoHasNoResult() {
        when(kakaoLocalClient.geocode(ADDRESS)).thenReturn(Optional.empty());

        assertTrue(geocoder.locate(ADDRESS).isEmpty());
    }

    @Test
    void emptyWhenKakaoFails() {
        when(kakaoLocalClient.geocode(ADDRESS)).thenThrow(new KakaoApiException("down", null));

        assertTrue(geocoder.locate(ADDRESS).isEmpty());
    }

    /** 기부 물품 위치는 대개 기부자 집 주소라 실패 로그에 그대로 남기지 않는다 (시·도, 시·군·구까지만). */
    @Test
    void logsOnlyRegionWhenKakaoHasNoResult(CapturedOutput output) {
        String home = "경기 부천시 길주로 210 101동 1001호";
        when(kakaoLocalClient.geocode(home)).thenReturn(Optional.empty());

        geocoder.locate(home);

        assertTrue(output.getOut().contains("경기 부천시"));
        assertFalse(output.getOut().contains("길주로"));
    }

    @Test
    void logsOnlyRegionWhenKakaoFails(CapturedOutput output) {
        String home = "경기 부천시 길주로 210 101동 1001호";
        ResourceAccessException cause = new ResourceAccessException(
                "I/O error on GET request for \"https://dapi.kakao.com/v2/local/search/address.json?query=" + home + "\"");
        when(kakaoLocalClient.geocode(home)).thenThrow(new KakaoApiException("카카오 주소 검색 API 호출 실패", cause));

        geocoder.locate(home);

        assertTrue(output.getOut().contains("경기 부천시"));
        assertTrue(output.getOut().contains("카카오 주소 검색 API 호출 실패"));
        assertFalse(output.getOut().contains("길주로"));
    }
}
