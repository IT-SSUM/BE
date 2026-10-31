package com.itssum.domain.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.itssum.global.infra.kakao.GeoPoint;
import com.itssum.global.infra.kakao.KakaoApiException;
import com.itssum.global.infra.kakao.KakaoLocalClient;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

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
}
