package com.itssum.domain.notification.entity;

import com.itssum.domain.user.entity.User;
import com.itssum.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

/**
 * 회원가입 시 기본값(전부 ON)으로 1행 생성한다. 알림 항목은 Figma 확정 전 임시.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "notification_settings", comment = "알림 설정")
public class NotificationSetting extends BaseTimeEntity {

    @Id
    @Column(name = "user_id")
    private Long id;

    /** @MapsId 라서 user_id 컬럼 정의(COMMENT 포함)는 이 @JoinColumn 을 따른다. */
    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "FK_users_TO_notification_settings"),
            comment = "회원 ID (users PK 공유)")
    private User user;

    @ColumnDefault("true")
    @Column(nullable = false, columnDefinition = "BOOLEAN",
            comment = "매칭 알림 (제안·요청, 수락·거절, 예약, 전달 약속, 완료, 취소)")
    private boolean matchEnabled = true;

    @ColumnDefault("true")
    @Column(nullable = false, columnDefinition = "BOOLEAN", comment = "채팅 알림")
    private boolean chatEnabled = true;

    private NotificationSetting(User user) {
        this.user = user;
    }

    public static NotificationSetting createDefault(User user) {
        return new NotificationSetting(user);
    }
}
