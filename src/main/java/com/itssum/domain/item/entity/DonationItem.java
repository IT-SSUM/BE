package com.itssum.domain.item.entity;

import com.itssum.domain.user.entity.Donor;
import com.itssum.global.common.BaseTimeEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
        name = "donation_items",
        comment = "기부 물품",
        indexes = {
                @Index(name = "IDX_donation_items_status_created", columnList = "status, created_at"),
                @Index(name = "IDX_donation_items_region_status", columnList = "region_id, status")
        }
)
@AttributeOverride(name = "createdAt", column = @Column(
        name = "created_at", nullable = false, updatable = false, secondPrecision = 0, comment = "등록 일시"))
public class DonationItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "donation_item_id", comment = "기부 물품 ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donor_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_donors_TO_donation_items"), comment = "등록한 기부자")
    private Donor donor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_item_categories_TO_donation_items"), comment = "카테고리")
    private ItemCategory category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_regions_TO_donation_items"), comment = "물품 위치 (구 단위, 목록·상세에 공개)")
    private Region region;

    @Column(length = 255, comment = "물품 상세 위치 (물품 수정 화면, 비공개·채팅에서 협의)")
    private String pickupAddress;

    @Column(nullable = false, length = 50, comment = "품목 (어린이 도서, 생수 ...)")
    private String itemName;

    @Column(nullable = false, comment = "총 수량")
    private int quantity;

    @Column(nullable = false, length = 10, comment = "단위 (개, 권, 장, 벌 ...)")
    private String unit;

    @ColumnDefault("0")
    @Column(nullable = false, comment = "진행 수량 (수락~전달 중인 매칭 합계)")
    private int reservedQuantity = 0;

    @ColumnDefault("0")
    @Column(nullable = false, comment = "전달 완료 수량")
    private int completedQuantity = 0;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10, comment = "물품 상태: HIGH(상·미개봉) | MID(중·사용감 적음) | LOW(하·다회 사용)")
    private ConditionGrade conditionGrade;

    @Column(columnDefinition = "TEXT", comment = "설명 (선택)")
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @ColumnDefault("'SHARING'")
    @Column(nullable = false, length = 20,
            comment = "SHARING(나눔중: 가능 수량 남음) | RESERVED(예약중: 남은 수량 0) | COMPLETED(전달 완료)")
    private DonationItemStatus status = DonationItemStatus.SHARING;

    @Column(secondPrecision = 0, comment = "삭제 일시 (soft delete)")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "donationItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DonationItemImage> images = new ArrayList<>();

    @Builder
    private DonationItem(Donor donor, ItemCategory category, Region region, String pickupAddress,
                         String itemName, int quantity, String unit, ConditionGrade conditionGrade,
                         String description) {
        this.donor = donor;
        this.category = category;
        this.region = region;
        this.pickupAddress = pickupAddress;
        this.itemName = itemName;
        this.quantity = quantity;
        this.unit = unit;
        this.conditionGrade = conditionGrade;
        this.description = description;
    }

    /** 양방향 연관관계 동기화: 이미지는 이 메서드로만 추가한다 (cascade로 함께 저장). */
    public void addImage(String imageUrl, int sortOrder) {
        images.add(DonationItemImage.builder()
                .donationItem(this)
                .imageUrl(imageUrl)
                .sortOrder(sortOrder)
                .build());
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
