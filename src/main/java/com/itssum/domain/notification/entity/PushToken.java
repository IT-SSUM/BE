package com.itssum.domain.notification.entity;

import com.itssum.domain.user.entity.User;
import com.itssum.global.common.BaseTimeEntity;
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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 기기별 1행. 같은 토큰이 다른 계정으로 다시 등록되면 user_id 를 갱신한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "push_tokens",
        comment = "푸시 토큰",
        uniqueConstraints = @UniqueConstraint(name = "UQ_push_tokens_token", columnNames = "token")
)
@AttributeOverride(name = "createdAt", column = @Column(
        name = "created_at", nullable = false, updatable = false, secondPrecision = 0, comment = "등록 일시"))
@AttributeOverride(name = "updatedAt", column = @Column(
        name = "updated_at", nullable = false, secondPrecision = 0, comment = "갱신 일시 (마지막 등록 요청)"))
public class PushToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "push_token_id", comment = "푸시 토큰 ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_users_TO_push_tokens"), comment = "회원 ID")
    private User user;

    @Column(nullable = false, length = 255, comment = "FCM 등록 토큰")
    private String token;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20, comment = "ANDROID | IOS | WEB")
    private DeviceType deviceType;

    @Builder
    private PushToken(User user, String token, DeviceType deviceType) {
        this.user = user;
        this.token = token;
        this.deviceType = deviceType;
    }
}
