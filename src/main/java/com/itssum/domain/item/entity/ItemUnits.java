package com.itssum.domain.item.entity;

import java.util.List;

/**
 * 물품 단위는 이 목록에서만 고른다. 매칭은 단위가 같아야 성립하므로("개"와 "병"은 다른 물품),
 * 자유 입력으로 받으면 같은 물건이 표기 차이로 매칭·추천에서 빠진다.
 * DB 에는 한글 그대로 저장한다 (알림 문구가 수량 뒤에 바로 붙여 쓴다: "수건 30장이 …").
 */
public final class ItemUnits {

    public static final List<String> ALL = List.of("개", "권", "장", "벌", "세트", "병", "박스", "켤레", "기타");

    private ItemUnits() {
    }

    public static boolean isSupported(String unit) {
        return unit != null && ALL.contains(unit);
    }
}
