package com.itssum.domain.user.entity;

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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "terms_agreements", comment = "약관 동의 이력")
public class TermsAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "agreement_id", comment = "동의 이력 ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_users_TO_terms_agreements"), comment = "회원 ID")
    private User user;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30,
            comment = "SERVICE(서비스 이용약관) | PRIVACY(개인정보 수집·이용) | ORG_ELIGIBILITY(기관 가입 자격 확인)")
    private TermsType termsType;

    @Column(nullable = false, length = 20, comment = "동의한 약관 버전")
    private String termsVersion;

    @Column(nullable = false, secondPrecision = 0, comment = "동의 일시")
    private LocalDateTime agreedAt;

    @Builder
    private TermsAgreement(User user, TermsType termsType, String termsVersion, LocalDateTime agreedAt) {
        this.user = user;
        this.termsType = termsType;
        this.termsVersion = termsVersion;
        this.agreedAt = agreedAt;
    }
}
