package com.itssum.domain.item.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "item_categories", comment = "물품 카테고리")
public class ItemCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id", comment = "카테고리 ID")
    private Long id;

    @Column(nullable = false, length = 50, comment = "카테고리명 (도서·문구, 생활용품 ...)")
    private String name;

    @ColumnDefault("0")
    @Column(nullable = false, comment = "노출 순서")
    private int sortOrder = 0;

    @Builder
    private ItemCategory(String name, int sortOrder) {
        this.name = name;
        this.sortOrder = sortOrder;
    }
}
