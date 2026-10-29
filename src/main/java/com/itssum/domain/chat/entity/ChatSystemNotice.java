package com.itssum.domain.chat.entity;

public enum ChatSystemNotice {
    MATCH_CONNECTED;

    public String title(boolean donor) {
        return donor ? "나눔이 연결됐어요 🌱" : "물품 지원이 연결됐어요 🌱";
    }

    public String description() {
        return "전달 날짜·장소·방법을 함께 정해 주세요.";
    }
}
