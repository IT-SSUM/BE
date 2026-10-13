package com.itssum.domain.chat.entity;

import com.itssum.domain.user.entity.User;
import com.itssum.global.common.BaseCreatedTimeEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "chat_messages",
        comment = "채팅 메시지",
        indexes = @Index(name = "IDX_chat_messages_room_message", columnList = "chat_room_id, message_id")
)
@AttributeOverride(name = "createdAt", column = @Column(
        name = "created_at", nullable = false, updatable = false, secondPrecision = 0, comment = "전송 일시"))
public class ChatMessage extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id", comment = "메시지 ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_chat_rooms_TO_chat_messages"), comment = "채팅방")
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "sender_id",
            foreignKey = @ForeignKey(name = "FK_users_TO_chat_messages"), comment = "보낸 회원 (SYSTEM 메시지면 NULL)")
    private User sender;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @ColumnDefault("'TEXT'")
    @Column(nullable = false, length = 20,
            comment = "TEXT | IMAGE | SYSTEM(나눔 연결·나눔중 변경·수령 확인 안내)"
                    + " | APPOINTMENT(전달 약속 카드: matches 일정·장소·방법 참조)")
    private MessageType messageType = MessageType.TEXT;

    @Column(nullable = false, columnDefinition = "TEXT", comment = "메시지 내용 또는 이미지 URL")
    private String content;

    /** messageType 을 넘기지 않으면 DB 기본값과 같은 TEXT 로 저장된다. */
    @Builder
    private ChatMessage(ChatRoom chatRoom, User sender, MessageType messageType, String content) {
        this.chatRoom = chatRoom;
        this.sender = sender;
        this.messageType = messageType != null ? messageType : MessageType.TEXT;
        this.content = content;
    }
}
