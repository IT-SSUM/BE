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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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

    @Column(length = 255, comment = "물품 위치 (도로명 주소, 좌표 변환 대상. 기부자에게만 보이고 목록·상세에는 구 단위 지역만 공개)")
    private String pickupAddress;

    @Column(length = 100, comment = "물품 상세 주소 (선택, 등록 3/3 화면. 동·호수·전달 메모)")
    private String pickupAddressDetail;

    @Column(precision = 10, scale = 7, comment = "물품 위치 위도 (주소 지오코딩, 거리순 추천용)")
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7, comment = "물품 위치 경도")
    private BigDecimal longitude;

    @Column(nullable = false, length = 50, comment = "품목 (어린이 도서, 생수 ...)")
    private String itemName;

    @Column(nullable = false, comment = "총 수량")
    private int quantity;

    @Column(nullable = false, length = 10, comment = "단위 (개, 권, 장, 벌 ...)")
    private String unit;

    @ColumnDefault("0")
    @Column(nullable = false, comment = "예약중 거래에 임시 배정된 수량")
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
    @ColumnDefault("'RECRUITING'")
    @Column(nullable = false, length = 20,
            comment = "RECRUITING(모집중) | FULL(예약마감) | COMPLETED(전달 완료)")
    private DonationItemStatus status = DonationItemStatus.RECRUITING;

    @Column(secondPrecision = 0, comment = "삭제 일시 (soft delete)")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "donationItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder asc, id asc")
    private List<DonationItemImage> images = new ArrayList<>();

    @Builder
    private DonationItem(Donor donor, ItemCategory category, Region region, String pickupAddress,
                         String pickupAddressDetail, BigDecimal latitude, BigDecimal longitude,
                         String itemName, int quantity, String unit, ConditionGrade conditionGrade,
                         String description) {
        this.donor = donor;
        this.category = category;
        this.region = region;
        this.pickupAddress = pickupAddress;
        this.pickupAddressDetail = pickupAddressDetail;
        this.latitude = latitude;
        this.longitude = longitude;
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

    /** 사진 목록을 이 순서로 바꾼다. 첫 번째가 대표 사진(sortOrder 0)이다. 빠진 사진 행은 orphanRemoval 로 지워진다. */
    public void replaceImages(List<String> imageKeys) {
        images.clear();
        for (int i = 0; i < imageKeys.size(); i++) {
            addImage(imageKeys.get(i), i);
        }
    }

    /** 사진 S3 key 목록 (대표 사진부터). */
    public List<String> imageKeys() {
        return images.stream()
                .sorted(Comparator.comparingInt(DonationItemImage::getSortOrder))
                .map(DonationItemImage::getImageUrl)
                .toList();
    }

    public String representativeImageKey() {
        List<String> keys = imageKeys();
        return keys.isEmpty() ? null : keys.getFirst();
    }

    public void changeDescription(String description) {
        this.description = description;
    }

    /** 물품 위치를 바꾼다. 좌표는 새 주소로 다시 구한 값이며, 변환에 실패했으면 null 로 비운다. */
    public void relocate(String pickupAddress, BigDecimal latitude, BigDecimal longitude) {
        this.pickupAddress = pickupAddress;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void changePickupAddressDetail(String pickupAddressDetail) {
        this.pickupAddressDetail = pickupAddressDetail;
    }

    public boolean isOwnedBy(Long donorId) {
        return donor.getId().equals(donorId);
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public int availableQuantity() {
        return quantity - reservedQuantity - completedQuantity;
    }

    public void reserve(int amount) {
        reservedQuantity += amount;
        refreshStatus();
    }

    public void release(int amount) {
        reservedQuantity -= amount;
        refreshStatus();
    }

    public void complete(int amount) {
        reservedQuantity -= amount;
        completedQuantity += amount;
        refreshStatus();
    }

    private void refreshStatus() {
        status = completedQuantity == quantity ? DonationItemStatus.COMPLETED
                : availableQuantity() == 0 ? DonationItemStatus.FULL : DonationItemStatus.RECRUITING;
    }
}
