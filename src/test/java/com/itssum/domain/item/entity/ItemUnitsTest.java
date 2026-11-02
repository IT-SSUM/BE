package com.itssum.domain.item.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ItemUnitsTest {

    @Test
    void supportsOnlyListedUnits() {
        assertEquals(List.of("개", "권", "장", "벌", "세트", "병", "박스", "켤레", "기타"), ItemUnits.ALL);
        assertTrue(ItemUnits.isSupported("권"));
        assertFalse(ItemUnits.isSupported("책"));
        assertFalse(ItemUnits.isSupported(" 권"));
        assertFalse(ItemUnits.isSupported(null));
    }
}
