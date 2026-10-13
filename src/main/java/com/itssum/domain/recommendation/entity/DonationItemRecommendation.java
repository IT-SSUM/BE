package com.itssum.domain.recommendation.entity;

import com.itssum.domain.item.entity.DonationItem;
import com.itssum.domain.item.entity.NeededItem;
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
 * 필요 물품 → 추천 기부 물품 순위.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "donation_item_recommendations",
        comment = "AI 기부 물품 추천",
        uniqueConstraints = @UniqueConstraint(
                name = "UQ_item_rec_needed_item", columnNames = {"needed_item_id", "donation_item_id"})
)
public class DonationItemRecommendation extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommendation_id", comment = "추천 ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "needed_item_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_needed_items_TO_donation_item_recommendations"),
            comment = "기준 필요 물품")
    private NeededItem neededItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donation_item_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_donation_items_TO_donation_item_recommendations"),
            comment = "추천된 기부 물품")
    private DonationItem donationItem;

    @Column(nullable = false, comment = "추천 순위")
    private int rankNo;

    @Column(precision = 5, scale = 2, comment = "적합도 점수")
    private BigDecimal score;

    @Column(precision = 6, scale = 2, comment = "거리 (거리순 정렬용)")
    private BigDecimal distanceKm;

    @Column(length = 255, comment = "추천 사유")
    private String reason;

    @Builder
    private DonationItemRecommendation(NeededItem neededItem, DonationItem donationItem, int rankNo,
                                       BigDecimal score, BigDecimal distanceKm, String reason) {
        this.neededItem = neededItem;
        this.donationItem = donationItem;
        this.rankNo = rankNo;
        this.score = score;
        this.distanceKm = distanceKm;
        this.reason = reason;
    }
}
