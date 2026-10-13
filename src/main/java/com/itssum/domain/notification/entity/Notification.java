package com.itssum.domain.notification.entity;

import com.itssum.domain.user.entity.User;
import com.itssum.global.common.BaseCreatedTimeEntity;
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
import java.time.LocalDateTime;
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
        name = "notifications",
        comment = "알림",
        indexes = @Index(name = "IDX_notifications_receiver_read", columnList = "receiver_id, is_read")
)
public class Notification extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id", comment = "알림 ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_users_TO_notifications"), comment = "수신 회원")
    private User receiver;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30,
            comment = "MATCH_REQUESTED(새 제안·요청) | MATCH_RESERVED(수락) | MATCH_REJECTED"
                    + " | MATCH_SCHEDULED(전달 일정 확정) | MATCH_SHARING(전달 준비 시작)"
                    + " | MATCH_RECEIVE_REQUIRED(수령 확인 요청) | MATCH_COMPLETED | MATCH_CANCELED(취소·수량 복원) | CHAT")
    private NotificationType type;

    @Column(nullable = false, length = 100, comment = "알림 제목")
    private String title;

    @Column(length = 255, comment = "알림 본문")
    private String content;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20, comment = "이동 대상: MATCH | CHAT_ROOM | DONATION_ITEM | NEEDED_ITEM")
    private NotificationTargetType targetType;

    @Column(comment = "이동 대상 ID")
    private Long targetId;

    @ColumnDefault("false")
    @Column(nullable = false, columnDefinition = "BOOLEAN", comment = "읽음 여부")
    private boolean isRead = false;

    @Column(secondPrecision = 0, comment = "읽은 일시")
    private LocalDateTime readAt;

    @Builder
    private Notification(User receiver, NotificationType type, String title, String content,
                         NotificationTargetType targetType, Long targetId) {
        this.receiver = receiver;
        this.type = type;
        this.title = title;
        this.content = content;
        this.targetType = targetType;
        this.targetId = targetId;
    }
}
