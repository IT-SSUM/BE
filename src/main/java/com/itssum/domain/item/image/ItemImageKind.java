package com.itssum.domain.item.image;

/** 물품 사진 key 접두사. key 는 {접두사}/{올린 회원 ID}/{uuid}.{확장자} 이다 (물품은 사진을 올릴 때 아직 없다). */
public enum ItemImageKind {

    DONATION("donation-items"),
    NEEDED("needed-items");

    private final String prefix;

    ItemImageKind(String prefix) {
        this.prefix = prefix;
    }

    public String prefix() {
        return prefix;
    }
}
