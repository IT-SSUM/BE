package com.itssum.domain.item.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "regions",
        comment = "지역",
        uniqueConstraints = @UniqueConstraint(name = "UQ_regions_sido_sigungu", columnNames = {"sido", "sigungu"})
)
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "region_id", comment = "지역 ID")
    private Long id;

    @Column(nullable = false, length = 20, comment = "시·도 (서울)")
    private String sido;

    @Column(nullable = false, length = 30, comment = "시·군·구 (구로구)")
    private String sigungu;

    @Column(nullable = false, precision = 10, scale = 7, comment = "구 중심 위도")
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7, comment = "구 중심 경도")
    private BigDecimal longitude;

    @Builder
    private Region(String sido, String sigungu, BigDecimal latitude, BigDecimal longitude) {
        this.sido = sido;
        this.sigungu = sigungu;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
