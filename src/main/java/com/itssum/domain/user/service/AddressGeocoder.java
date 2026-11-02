package com.itssum.domain.user.service;

import com.itssum.global.infra.kakao.GeoPoint;
import com.itssum.global.infra.kakao.KakaoApiException;
import com.itssum.global.infra.kakao.KakaoLocalClient;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 기관 주소와 기부 물품 위치를 좌표(거리순 추천 기준)로 바꾼다. 좌표가 없으면 거리순 추천에서만 빠지므로,
 * 카카오 장애나 검색 결과 없음은 빈 값으로 돌려주고 가입·등록·수정은 그대로 진행하게 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AddressGeocoder {

    /** 로그에 남기는 주소 앞부분 (시·도, 시·군·구). */
    private static final int LOGGED_ADDRESS_PARTS = 2;

    private final KakaoLocalClient kakaoLocalClient;

    public Optional<GeoPoint> locate(String address) {
        try {
            Optional<GeoPoint> point = kakaoLocalClient.geocode(address);
            if (point.isEmpty()) {
                log.warn("주소 좌표 변환 결과 없음: {}", region(address));
            }
            return point;
        } catch (KakaoApiException e) {
            // 원인 예외 메시지에는 요청 URL(주소 포함)이 들어 있어서 남기지 않는다
            log.warn("주소 좌표 변환 실패: {} ({}, 원인 {})", region(address), e.getMessage(),
                    e.getCause() == null ? "없음" : e.getCause().getClass().getSimpleName());
            return Optional.empty();
        }
    }

    /** 기부 물품 위치는 대개 기부자 집 주소라 로그에는 시·도, 시·군·구까지만 남긴다. */
    private static String region(String address) {
        if (address == null) {
            return null;
        }
        return Arrays.stream(address.strip().split("\\s+"))
                .limit(LOGGED_ADDRESS_PARTS)
                .collect(Collectors.joining(" ")) + " …";
    }
}
