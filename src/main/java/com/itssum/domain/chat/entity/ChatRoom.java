package com.itssum.domain.chat.entity;

import com.itssum.domain.match.entity.Match;
import com.itssum.global.common.BaseCreatedTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "chat_rooms",
        comment = "채팅방",
        uniqueConstraints = @UniqueConstraint(name = "UQ_chat_rooms_match", columnNames = "match_id")
)
public class ChatRoom extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_id", comment = "채팅방 ID")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_matches_TO_chat_rooms"), comment = "개별 거래 1건당 채팅방 1개 (상대 수락 시 생성)")
    private Match match;

    @Column(length = 255, comment = "목록 미리보기용 마지막 메시지")
    private String lastMessageContent;

    @Column(secondPrecision = 0, comment = "목록 정렬용 마지막 메시지 시각")
    private LocalDateTime lastMessageAt;

    @Builder
    private ChatRoom(Match match) {
        this.match = match;
    }

    public void updateLastMessage(String content, LocalDateTime sentAt) {
        this.lastMessageContent = content;
        this.lastMessageAt = sentAt;
    }
}
