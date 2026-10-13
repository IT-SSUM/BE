package com.itssum.domain.item.entity;

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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "donation_item_images", comment = "기부 물품 이미지")
@AttributeOverride(name = "createdAt", column = @Column(
        name = "created_at", nullable = false, updatable = false, secondPrecision = 0, comment = "등록 일시"))
public class DonationItemImage extends BaseCreatedTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id", comment = "이미지 ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donation_item_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_donation_items_TO_donation_item_images"), comment = "기부 물품 ID")
    private DonationItem donationItem;

    @Column(nullable = false, length = 500, comment = "S3 URL (최소 1장)")
    private String imageUrl;

    @ColumnDefault("0")
    @Column(nullable = false, comment = "0 = 대표 이미지")
    private int sortOrder = 0;

    @Builder
    private DonationItemImage(DonationItem donationItem, String imageUrl, int sortOrder) {
        this.donationItem = donationItem;
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
    }
}
