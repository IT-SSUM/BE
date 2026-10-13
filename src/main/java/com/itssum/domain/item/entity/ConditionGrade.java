package com.itssum.domain.item.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * donation_items.condition_grade, needed_items.desired_condition 공용.
 * needed_items 에서는 "이 등급 이상"(희망 최소 상태)의 의미로 쓴다.
 */
@Getter
@RequiredArgsConstructor
public enum ConditionGrade {

    HIGH("상·미개봉"),
    MID("중·사용감 적음"),
    LOW("하·다회 사용");

    private final String description;
}
