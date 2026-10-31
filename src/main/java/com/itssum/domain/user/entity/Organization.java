package com.itssum.domain.user.entity;

import com.itssum.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
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
        name = "organizations",
        comment = "복지기관",
        uniqueConstraints = @UniqueConstraint(name = "UQ_organizations_business_number", columnNames = "business_number")
)
public class Organization extends BaseTimeEntity {

    @Id
    @Column(name = "user_id")
    private Long id;

    /** @MapsId 라서 user_id 컬럼 정의(COMMENT 포함)는 이 @JoinColumn 을 따른다. */
    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "FK_users_TO_organizations"),
            comment = "회원 ID (users PK 공유, API의 organizationId)")
    private User user;

    @Column(nullable = false, length = 100, comment = "기관명")
    private String name;

    @Column(nullable = false, length = 50, comment = "대표자명 (NTS 진위확인 p_nm)")
    private String representativeName;

    @Column(nullable = false, columnDefinition = "CHAR(10)", comment = "사업자번호 10자리, 하이픈 제외 (NTS b_no)")
    private String businessNumber;

    @Column(nullable = false, comment = "개업일자 (NTS start_dt)")
    private LocalDate openingDate;

    @Column(nullable = false, length = 20, comment = "기관 연락처")
    private String phone;

    @Column(nullable = false, length = 255, comment = "기관 주소 (우편번호 검색 결과 도로명 주소)")
    private String address;

    @Column(length = 100, comment = "상세주소 (동·호수, 선택)")
    private String addressDetail;

    @Column(length = 255, comment = "기관 소개 (필요 물품 상세·기관 상세에 노출)")
    private String introduction;

    @Column(precision = 10, scale = 7, comment = "위도 (주소 지오코딩, 거리순 추천용)")
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7, comment = "경도")
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @ColumnDefault("'VERIFIED'")
    @Column(nullable = false, length = 20, comment = "VERIFIED | SUSPENDED(상태조회로 휴·폐업 감지)")
    private VerificationStatus verificationStatus = VerificationStatus.VERIFIED;

    @Column(nullable = false, secondPrecision = 0, comment = "사업자 확인 일시")
    private LocalDateTime verifiedAt;

    @Builder
    private Organization(User user, String name, String representativeName, String businessNumber,
                         LocalDate openingDate, String phone, String address, String addressDetail,
                         String introduction, BigDecimal latitude, BigDecimal longitude, LocalDateTime verifiedAt) {
        this.user = user;
        this.name = name;
        this.representativeName = representativeName;
        this.businessNumber = businessNumber;
        this.openingDate = openingDate;
        this.phone = phone;
        this.address = address;
        this.addressDetail = addressDetail;
        this.introduction = introduction;
        this.latitude = latitude;
        this.longitude = longitude;
        this.verifiedAt = verifiedAt;
    }
}
