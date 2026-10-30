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

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "refresh_tokens",
        comment = "리프레시 토큰",
        uniqueConstraints = @UniqueConstraint(name = "UQ_refresh_tokens_hash", columnNames = "token_hash")
)
@AttributeOverride(name = "createdAt", column = @Column(
        name = "created_at", nullable = false, updatable = false, secondPrecision = 0, comment = "발급 일시"))
public class RefreshToken extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refresh_token_id", comment = "리프레시 토큰 ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_users_TO_refresh_tokens"), comment = "회원 ID")
    private User user;

    @Column(nullable = false, columnDefinition = "CHAR(64)", comment = "리프레시 토큰 SHA-256 해시")
    private String tokenHash;

    @Column(nullable = false, secondPrecision = 0, comment = "만료 일시")
    private LocalDateTime expiresAt;

    @Builder
    private RefreshToken(User user, String tokenHash, LocalDateTime expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }
}
