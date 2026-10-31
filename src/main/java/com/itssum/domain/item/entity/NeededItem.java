package com.itssum.domain.item.entity;

import com.itssum.domain.user.entity.Organization;
import com.itssum.global.common.BaseTimeEntity;
import jakarta.persistence.AttributeOverride;
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
import jakarta.persistence.Table;
import java.math.BigDecimal;
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
        name = "needed_items",
        comment = "필요 물품",
        indexes = {
                @Index(name = "IDX_needed_items_status_created", columnList = "status, created_at"),
                @Index(name = "IDX_needed_items_region_status", columnList = "region_id, status")
        }
)
@AttributeOverride(name = "createdAt", column = @Column(
        name = "created_at", nullable = false, updatable = false, secondPrecision = 0, comment = "등록 일시"))
public class NeededItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "needed_item_id", comment = "필요 물품 ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_organizations_TO_needed_items"), comment = "등록한 기관")
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_item_categories_TO_needed_items"), comment = "카테고리")
    private ItemCategory category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_regions_TO_needed_items"), comment = "수령 지역 (구 단위, 목록·필터에 공개)")
    private Region region;

    @Column(nullable = false, length = 50, comment = "품목")
    private String itemName;

    @Column(nullable = false, comment = "필요 수량")
    private int requiredQuantity;

    @Column(nullable = false, length = 10, comment = "단위")
    private String unit;

    @ColumnDefault("0")
    @Column(nullable = false, comment = "예약(진행) 수량")
    private int reservedQuantity = 0;

    @ColumnDefault("0")
    @Column(nullable = false, comment = "수령 완료 수량")
    private int receivedQuantity = 0;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10, comment = "희망 최소 상태: HIGH | MID | LOW (\"중 이상\"처럼 이 등급 이상)")
    private ConditionGrade desiredCondition;

    @Column(columnDefinition = "TEXT", comment = "필요한 이유 / 설명 (선택)")
    private String description;

    @Column(length = 500, comment = "참고 사진 S3 URL (선택, 1장)")
    private String referenceImageUrl;

    @Column(length = 255, comment = "수령 장소 상세 주소 (선택, 필요 나눔 수정 화면)")
    private String receiveAddress;

    @Column(precision = 10, scale = 7, comment = "수령 장소 위도")
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7, comment = "수령 장소 경도")
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @ColumnDefault("'RECRUITING'")
    @Column(nullable = false, length = 20,
            comment = "RECRUITING(모집중) | FULL(예약마감) | CLOSED(모집 종료)")
    private NeededItemStatus status = NeededItemStatus.RECRUITING;

    @Column(secondPrecision = 0, comment = "모집 종료 일시")
    private LocalDateTime closedAt;

    @Column(secondPrecision = 0, comment = "삭제 일시 (soft delete)")
    private LocalDateTime deletedAt;

    @Builder
    private NeededItem(Organization organization, ItemCategory category, Region region, String itemName,
                       int requiredQuantity, String unit, ConditionGrade desiredCondition, String description,
                       String referenceImageUrl, String receiveAddress, BigDecimal latitude, BigDecimal longitude) {
        this.organization = organization;
        this.category = category;
        this.region = region;
        this.itemName = itemName;
        this.requiredQuantity = requiredQuantity;
        this.unit = unit;
        this.desiredCondition = desiredCondition;
        this.description = description;
        this.referenceImageUrl = referenceImageUrl;
        this.receiveAddress = receiveAddress;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public int availableQuantity() {
        return requiredQuantity - reservedQuantity - receivedQuantity;
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
        receivedQuantity += amount;
        refreshStatus();
    }

    private void refreshStatus() {
        if (receivedQuantity == requiredQuantity) {
            status = NeededItemStatus.CLOSED;
            closedAt = LocalDateTime.now();
        } else if (status != NeededItemStatus.CLOSED) {
            status = availableQuantity() == 0 ? NeededItemStatus.FULL : NeededItemStatus.RECRUITING;
        }
    }
}
