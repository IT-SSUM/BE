package com.itssum.domain.auth.entity;

import com.itssum.domain.user.entity.User;
import com.itssum.global.common.BaseCreatedTimeEntity;
import jakarta.persistence.AttributeOverride;
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

/**
 * 메일 링크의 토큰은 원문 대신 SHA-256 해시만 저장하고, 1회 사용 후 used_at 을 기록한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "password_reset_tokens",
        comment = "비밀번호 재설정 토큰",
        uniqueConstraints = @UniqueConstraint(name = "UQ_password_reset_tokens_hash", columnNames = "token_hash")
)
@AttributeOverride(name = "createdAt", column = @Column(
        name = "created_at", nullable = false, updatable = false, secondPrecision = 0, comment = "발급 일시"))
public class PasswordResetToken extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reset_token_id", comment = "재설정 토큰 ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_users_TO_password_reset_tokens"), comment = "회원 ID")
    private User user;

    @Column(nullable = false, columnDefinition = "CHAR(64)", comment = "재설정 토큰 SHA-256 해시")
    private String tokenHash;

    @Column(nullable = false, secondPrecision = 0, comment = "만료 일시 (발급 후 30분 등)")
    private LocalDateTime expiresAt;

    @Column(secondPrecision = 0, comment = "사용 일시 (NULL = 미사용)")
    private LocalDateTime usedAt;

    @Builder
    private PasswordResetToken(User user, String tokenHash, LocalDateTime expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }
}
