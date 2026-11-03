package com.itssum.domain.item.service;

/** 물품 요청의 선택 입력값 정리. */
final class ItemTexts {

    private ItemTexts() {
    }

    /** 선택 항목은 비어 있으면 저장하지 않는다 (수정에서는 "비우기"). 저장 전에 앞뒤 공백을 자른다. */
    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
