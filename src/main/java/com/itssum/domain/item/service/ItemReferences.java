package com.itssum.domain.item.service;

import com.itssum.domain.item.entity.ItemCategory;
import com.itssum.domain.item.entity.ItemUnits;
import com.itssum.domain.item.entity.Region;
import com.itssum.domain.item.repository.ItemCategoryRepository;
import com.itssum.domain.item.repository.RegionRepository;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 기부 물품·필요 물품 등록이 함께 쓰는 기준 데이터 확인 (카테고리, 지역, 단위). */
@Component
@RequiredArgsConstructor
public class ItemReferences {

    private final ItemCategoryRepository itemCategoryRepository;
    private final RegionRepository regionRepository;

    public ItemCategory requireCategory(Long categoryId) {
        return itemCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ITEM_CATEGORY_NOT_FOUND));
    }

    /** 지역을 고르지 않으면 파일럿 지역(경기 부천시). 기부자 회원가입과 같은 규칙이다 (#14). */
    public Region requirePilotRegion(Long regionId) {
        long id = regionId == null ? Region.PILOT_REGION_ID : regionId;
        Region region = regionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.REGION_NOT_FOUND));
        if (id != Region.PILOT_REGION_ID) {
            throw new BusinessException(ErrorCode.REGION_NOT_SUPPORTED);
        }
        return region;
    }

    public String requireUnit(String unit) {
        if (!ItemUnits.isSupported(unit)) {
            throw new BusinessException(ErrorCode.ITEM_UNIT_NOT_SUPPORTED);
        }
        return unit;
    }
}
