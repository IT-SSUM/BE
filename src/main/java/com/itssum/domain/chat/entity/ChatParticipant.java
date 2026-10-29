package com.itssum.domain.chat.entity;

import com.itssum.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
        name = "chat_participants",
        comment = "채팅 참여자",
        uniqueConstraints = @UniqueConstraint(
                name = "UQ_chat_participants_room_user", columnNames = {"chat_room_id", "user_id"})
)
public class ChatParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_participant_id", comment = "참여 ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_chat_rooms_TO_chat_participants"), comment = "채팅방")
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_users_TO_chat_participants"), comment = "참여 회원 (기부자 또는 기관)")
    private User user;

    /** FK 아님. chat_messages.message_id 값만 보관한다. */
    @Column(comment = "마지막으로 읽은 메시지 ID (안읽음 수 계산용)")
    private Long lastReadMessageId;

    @Column(nullable = false, secondPrecision = 0, comment = "참여 일시")
    private LocalDateTime joinedAt;

    @Builder
    private ChatParticipant(ChatRoom chatRoom, User user, LocalDateTime joinedAt) {
        this.chatRoom = chatRoom;
        this.user = user;
        this.joinedAt = joinedAt;
    }

    public void markReadThrough(Long messageId) {
        if (lastReadMessageId == null || messageId > lastReadMessageId) {
            lastReadMessageId = messageId;
        }
    }
}
