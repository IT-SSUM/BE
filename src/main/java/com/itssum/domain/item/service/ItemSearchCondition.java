package com.itssum.domain.item.service;

import com.itssum.domain.item.entity.ConditionGrade;

/** 탐색 목록 필터. 모두 선택이며 null 이면 그 조건을 걸지 않는다. */
public record ItemSearchCondition(String keyword, Long categoryId, Long regionId, ConditionGrade condition) {

    /** 리포지토리 쿼리의 ESCAPE 문자. MySQL 문자열에서 역슬래시는 따로 이스케이프돼서 '!'를 쓴다. */
    public static final char LIKE_ESCAPE = '!';

    /**
     * 검색어를 포함 검색 LIKE 패턴으로 바꾼다. 비어 있으면 null (필터 없음).
     * '%'·'_'는 와일드카드가 아니라 글자 그대로 찾도록 이스케이프한다 ("100%"가 모든 물품을 고르지 않게).
     */
    public String likePattern() {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String escaped = keyword.strip()
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }
}
