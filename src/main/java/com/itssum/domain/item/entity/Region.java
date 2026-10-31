package com.itssum.domain.item.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기부자 활동 지역·물품 지역 표시용 ("경기 부천시"). 거리 계산은 물품·기관 주소의 좌표로 하므로 좌표를 두지 않는다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "regions",
        comment = "지역",
        uniqueConstraints = @UniqueConstraint(name = "UQ_regions_sido_sigungu", columnNames = {"sido", "sigungu"})
)
public class Region {

    /** 파일럿 지역 경기 부천시. data.sql 이 이 ID 로 넣고, 지역을 고르지 않으면 이 지역으로 저장한다. */
    public static final long PILOT_REGION_ID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "region_id", comment = "지역 ID")
    private Long id;

    @Column(nullable = false, length = 20, comment = "시·도 (경기)")
    private String sido;

    @Column(nullable = false, length = 30, comment = "시·군·구 (부천시)")
    private String sigungu;

    @Builder
    private Region(String sido, String sigungu) {
        this.sido = sido;
        this.sigungu = sigungu;
    }
}
