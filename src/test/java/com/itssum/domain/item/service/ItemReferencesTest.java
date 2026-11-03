package com.itssum.domain.item.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.itssum.domain.item.entity.ItemCategory;
import com.itssum.domain.item.entity.Region;
import com.itssum.domain.item.repository.ItemCategoryRepository;
import com.itssum.domain.item.repository.RegionRepository;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ItemReferencesTest {

    private final ItemCategoryRepository categories = mock(ItemCategoryRepository.class);
    private final RegionRepository regions = mock(RegionRepository.class);
    private final ItemReferences references = new ItemReferences(categories, regions);

    @Test
    void missingCategoryIs404() {
        when(categories.findById(99L)).thenReturn(Optional.empty());

        assertEquals(ErrorCode.ITEM_CATEGORY_NOT_FOUND,
                assertThrows(BusinessException.class, () -> references.requireCategory(99L)).getErrorCode());
    }

    @Test
    void existingCategoryIsReturned() {
        ItemCategory category = mock(ItemCategory.class);
        when(categories.findById(5L)).thenReturn(Optional.of(category));

        assertSame(category, references.requireCategory(5L));
    }

    @Test
    void omittedRegionIsPilotRegion() {
        Region pilot = mock(Region.class);
        when(regions.findById(Region.PILOT_REGION_ID)).thenReturn(Optional.of(pilot));

        assertSame(pilot, references.requirePilotRegion(null));
        assertSame(pilot, references.requirePilotRegion(1L));
    }

    @Test
    void otherRegionIsNotSupported() {
        when(regions.findById(2L)).thenReturn(Optional.of(mock(Region.class)));
        when(regions.findById(3L)).thenReturn(Optional.empty());

        assertEquals(ErrorCode.REGION_NOT_SUPPORTED,
                assertThrows(BusinessException.class, () -> references.requirePilotRegion(2L)).getErrorCode());
        assertEquals(ErrorCode.REGION_NOT_FOUND,
                assertThrows(BusinessException.class, () -> references.requirePilotRegion(3L)).getErrorCode());
    }

    @Test
    void unitMustBeListed() {
        assertEquals("권", references.requireUnit("권"));
        assertEquals(ErrorCode.ITEM_UNIT_NOT_SUPPORTED,
                assertThrows(BusinessException.class, () -> references.requireUnit("책")).getErrorCode());
        assertEquals(ErrorCode.ITEM_UNIT_NOT_SUPPORTED,
                assertThrows(BusinessException.class, () -> references.requireUnit(null)).getErrorCode());
    }
}
