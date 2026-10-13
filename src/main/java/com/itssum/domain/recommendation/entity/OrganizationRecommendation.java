package com.itssum.domain.recommendation.entity;

import com.itssum.domain.item.entity.DonationItem;
import com.itssum.domain.item.entity.NeededItem;
import com.itssum.domain.user.entity.Organization;
import com.itssum.global.common.BaseCreatedTimeEntity;
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
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기부 물품 → 추천 기관 순위. 재추천 시 기존 행을 삭제하고 다시 생성한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "organization_recommendations",
        comment = "AI 기관 추천",
        uniqueConstraints = @UniqueConstraint(
                name = "UQ_org_rec_item_needed", columnNames = {"donation_item_id", "needed_item_id"})
)
public class OrganizationRecommendation extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommendation_id", comment = "추천 ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donation_item_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_donation_items_TO_organization_recommendations"),
            comment = "기준 기부 물품")
    private DonationItem donationItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "needed_item_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_needed_items_TO_organization_recommendations"),
            comment = "추천된 기관의 필요 물품")
    private NeededItem neededItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_organizations_TO_organization_recommendations"),
            comment = "추천된 기관 (목록 조회용)")
    private Organization organization;

    @Column(nullable = false, comment = "추천 순위")
    private int rankNo;

    @Column(precision = 5, scale = 2, comment = "적합도 점수")
    private BigDecimal score;

    @Column(precision = 6, scale = 2, comment = "거리 (거리순 정렬용)")
    private BigDecimal distanceKm;

    @Column(length = 255, comment = "추천 사유 (품목 수량 일치, 가까운 거리, 상태 적합 ...)")
    private String reason;

    @Builder
    private OrganizationRecommendation(DonationItem donationItem, NeededItem neededItem, Organization organization,
                                       int rankNo, BigDecimal score, BigDecimal distanceKm, String reason) {
        this.donationItem = donationItem;
        this.neededItem = neededItem;
        this.organization = organization;
        this.rankNo = rankNo;
        this.score = score;
        this.distanceKm = distanceKm;
        this.reason = reason;
    }
}
