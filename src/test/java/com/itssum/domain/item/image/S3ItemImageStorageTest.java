package com.itssum.domain.item.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itssum.global.infra.s3.PresignedImageUpload;
import com.itssum.global.infra.s3.S3ImageStorage;
import com.itssum.global.infra.s3.S3ObjectStorage;
import org.junit.jupiter.api.Test;

class S3ItemImageStorageTest {

    private final S3ObjectStorage storage = mock(S3ObjectStorage.class);
    private final S3ItemImageStorage images = new S3ItemImageStorage(new S3ImageStorage(storage));

    @Test
    void donationUploadKeyIsScopedToDonor() {
        when(storage.presignUpload(anyString(), eq("image/png"))).thenReturn("https://example.com/upload");

        PresignedImageUpload result = images.presignUpload(ItemImageKind.DONATION, 7L, "image/png");

        assertEquals("https://example.com/upload", result.uploadUrl());
        assertTrue(result.objectKey().matches("donation-items/7/[0-9a-f-]{36}\\.png"));
    }

    @Test
    void neededUploadKeyIsScopedToOrganization() {
        when(storage.presignUpload(anyString(), eq("image/jpeg"))).thenReturn("https://example.com/upload");

        PresignedImageUpload result = images.presignUpload(ItemImageKind.NEEDED, 9L, "image/jpeg");

        assertTrue(result.objectKey().matches("needed-items/9/[0-9a-f-]{36}\\.jpg"));
    }

    @Test
    void anotherOwnersKeyIsNotUploaded() {
        String key = "donation-items/8/11111111-1111-1111-1111-111111111111.jpg";

        assertFalse(images.isUploaded(ItemImageKind.DONATION, 7L, key));
        verify(storage, never()).find(key);
    }

    @Test
    void neededKeyIsNotADonationImage() {
        String key = "needed-items/7/11111111-1111-1111-1111-111111111111.jpg";

        assertFalse(images.isUploaded(ItemImageKind.DONATION, 7L, key));
        verify(storage, never()).find(key);
    }
}
