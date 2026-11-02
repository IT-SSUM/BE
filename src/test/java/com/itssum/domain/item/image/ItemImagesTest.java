package com.itssum.domain.item.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class ItemImagesTest {

    private final ItemImageStorage storage = mock(ItemImageStorage.class);

    @Test
    void writeWithoutStorageIsUnavailable() {
        ItemImages images = new ItemImages(provider(null));

        assertEquals(ErrorCode.ITEM_IMAGE_STORAGE_UNAVAILABLE, assertThrows(BusinessException.class,
                () -> images.presignUpload(ItemImageKind.DONATION, 7L, "image/jpeg")).getErrorCode());
        assertEquals(ErrorCode.ITEM_IMAGE_STORAGE_UNAVAILABLE, assertThrows(BusinessException.class,
                () -> images.verifyUploaded(ItemImageKind.DONATION, 7L, List.of("k"))).getErrorCode());
    }

    @Test
    void emptyKeysNeedNoStorage() {
        ItemImages images = new ItemImages(provider(null));

        images.verifyUploaded(ItemImageKind.DONATION, 7L, List.of());
    }

    @Test
    void readWithoutStorageIsNull() {
        ItemImages images = new ItemImages(provider(null));

        assertNull(images.readUrl("donation-items/7/a.jpg"));
        assertNull(images.readUrl(null));
    }

    @Test
    void readSignsKeyWhenStorageExists() {
        when(storage.presignRead("donation-items/7/a.jpg")).thenReturn("https://example.com/a");
        ItemImages images = new ItemImages(provider(storage));

        assertEquals("https://example.com/a", images.readUrl("donation-items/7/a.jpg"));
        assertNull(images.readUrl(null));
    }

    @Test
    void rejectsWhenAnyKeyIsNotUploaded() {
        when(storage.isUploaded(ItemImageKind.DONATION, 7L, "a")).thenReturn(true);
        when(storage.isUploaded(ItemImageKind.DONATION, 7L, "b")).thenReturn(false);
        ItemImages images = new ItemImages(provider(storage));

        assertEquals(ErrorCode.ITEM_IMAGE_NOT_UPLOADED, assertThrows(BusinessException.class,
                () -> images.verifyUploaded(ItemImageKind.DONATION, 7L, List.of("a", "b"))).getErrorCode());
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<ItemImageStorage> provider(ItemImageStorage storage) {
        ObjectProvider<ItemImageStorage> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(storage);
        return provider;
    }
}
