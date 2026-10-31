package com.itssum.domain.user.service;

import com.itssum.global.infra.kakao.GeoPoint;
import com.itssum.global.infra.kakao.KakaoApiException;
import com.itssum.global.infra.kakao.KakaoLocalClient;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 기관 주소를 좌표(거리순 추천 기준)로 바꾼다. 좌표가 없으면 거리순 추천에서만 빠지므로,
 * 카카오 장애나 검색 결과 없음은 빈 값으로 돌려주고 가입·수정은 그대로 진행하게 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AddressGeocoder {

    private final KakaoLocalClient kakaoLocalClient;

    public Optional<GeoPoint> locate(String address) {
        try {
            Optional<GeoPoint> point = kakaoLocalClient.geocode(address);
            if (point.isEmpty()) {
                log.warn("주소 좌표 변환 결과 없음: {}", address);
            }
            return point;
        } catch (KakaoApiException e) {
            log.warn("주소 좌표 변환 실패: {}", address, e);
            return Optional.empty();
        }
    }
}
