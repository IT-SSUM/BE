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
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 국세청 사업자등록정보 진위확인·상태조회 API 호출 이력.
 * valid_code, business_status_code는 국세청 응답 코드 원문(CHAR(2))을 그대로 저장하므로 enum으로 바꾸지 않는다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "organization_verifications", comment = "기관 사업자 인증 이력")
public class OrganizationVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "verification_id", comment = "인증 이력 ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "organization_id",
            foreignKey = @ForeignKey(name = "FK_organizations_TO_organization_verifications"),
            comment = "기관 ID (가입 전 사업자 확인 단계에선 NULL, 가입 완료 시 연결)")
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20, comment = "VALIDATE(진위확인) | STATUS(상태조회)")
    private VerificationType verificationType;

    @Column(nullable = false, columnDefinition = "CHAR(10)", comment = "요청 사업자번호 (b_no)")
    private String businessNumber;

    @Column(columnDefinition = "CHAR(2)", comment = "진위확인 결과 valid: 01 일치 | 02 불일치")
    private String validCode;

    @Column(columnDefinition = "CHAR(2)", comment = "상태 b_stt_cd: 01 계속 | 02 휴업 | 03 폐업")
    private String businessStatusCode;

    @Column(length = 100, comment = "과세유형 메시지 (tax_type)")
    private String taxType;

    @Column(comment = "폐업일 (end_dt)")
    private LocalDate closedDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20, comment = "SUCCESS | FAIL | ERROR(API 장애)")
    private VerificationResult result;

    @Column(nullable = false, secondPrecision = 0, comment = "조회 일시")
    private LocalDateTime checkedAt;

    @Builder
    private OrganizationVerification(Organization organization, VerificationType verificationType,
                                     String businessNumber, String validCode, String businessStatusCode,
                                     String taxType, LocalDate closedDate, VerificationResult result,
                                     LocalDateTime checkedAt) {
        this.organization = organization;
        this.verificationType = verificationType;
        this.businessNumber = businessNumber;
        this.validCode = validCode;
        this.businessStatusCode = businessStatusCode;
        this.taxType = taxType;
        this.closedDate = closedDate;
        this.result = result;
        this.checkedAt = checkedAt;
    }

    /** 가입 전 사업자 확인 이력을 가입한 기관에 연결한다. */
    public void linkOrganization(Organization organization) {
        this.organization = organization;
    }
}
