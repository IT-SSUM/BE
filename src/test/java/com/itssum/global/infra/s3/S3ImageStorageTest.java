package com.itssum.global.infra.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import com.itssum.global.infra.s3.S3ObjectStorage.StoredObjectMetadata;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class S3ImageStorageTest {

    private static final String UUID = "11111111-1111-1111-1111-111111111111";

    private final S3ObjectStorage objectStorage = mock(S3ObjectStorage.class);
    private final S3ImageStorage images = new S3ImageStorage(objectStorage);

    @Test
    void uploadKeyIsScopedToPrefixAndOwner() {
        when(objectStorage.presignUpload(anyString(), eq("image/webp"))).thenReturn("https://example.com/upload");

        PresignedImageUpload result = images.presignUpload("donation-items", 7L, "image/webp");

        assertTrue(result.objectKey().matches("donation-items/7/[0-9a-f-]{36}\\.webp"));
        assertEquals("https://example.com/upload", result.uploadUrl());
        assertEquals("image/webp", result.requiredHeaders().get("Content-Type"));
        assertEquals("*", result.requiredHeaders().get("If-None-Match"));
        verify(objectStorage).presignUpload(result.objectKey(), "image/webp");
    }

    @Test
    void unsupportedTypeIsRejected() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> images.presignUpload("chat", 42L, "image/gif"));

        assertEquals(ErrorCode.INVALID_INPUT, error.getErrorCode());
        verifyNoInteractions(objectStorage);
    }

    @Test
    void keyOfAnotherOwnerIsRejectedWithoutCallingS3() {
        assertFalse(images.verifyUploaded("chat", 42L, "chat/43/" + UUID + ".jpg"));
        assertFalse(images.verifyUploaded("chat", 42L, "chat/42/../" + UUID + ".jpg"));
        assertFalse(images.verifyUploaded("chat", 42L, "chat/42/" + UUID + ".gif"));
        assertFalse(images.verifyUploaded("chat", 42L, null));

        verifyNoInteractions(objectStorage);
    }

    @Test
    void missingObjectIsRejected() {
        String key = "chat/42/" + UUID + ".jpg";
        when(objectStorage.find(key)).thenReturn(Optional.empty());

        assertFalse(images.verifyUploaded("chat", 42L, key));
    }

    @Test
    void oversizedOrWrongTypeObjectIsDeleted() {
        String oversized = "chat/42/" + UUID + ".jpg";
        String wrongType = "chat/42/" + UUID + ".png";
        when(objectStorage.find(oversized))
                .thenReturn(Optional.of(new StoredObjectMetadata(11L * 1024 * 1024, "image/jpeg", "etag-1")));
        when(objectStorage.find(wrongType))
                .thenReturn(Optional.of(new StoredObjectMetadata(1024L, "text/plain", "etag-2")));

        assertFalse(images.verifyUploaded("chat", 42L, oversized));
        assertFalse(images.verifyUploaded("chat", 42L, wrongType));

        verify(objectStorage).delete(oversized, "etag-1");
        verify(objectStorage).delete(wrongType, "etag-2");
    }

    @Test
    void validUploadPasses() {
        String key = "chat/42/" + UUID + ".jpg";
        when(objectStorage.find(key))
                .thenReturn(Optional.of(new StoredObjectMetadata(1024L * 1024, "image/jpeg", "etag-1")));

        assertTrue(images.verifyUploaded("chat", 42L, key));
        verify(objectStorage, never()).delete(anyString(), anyString());
    }

    @Test
    void thumbnailKeyReplacesExtensionWithWebp() {
        assertEquals("thumbnails/chat/12/" + UUID + ".webp", S3ImageStorage.thumbnailKeyOf("chat/12/" + UUID + ".jpg"));
        assertEquals("thumbnails/chat/12/" + UUID + ".webp", S3ImageStorage.thumbnailKeyOf("chat/12/" + UUID + ".png"));
        assertEquals("thumbnails/chat/12/" + UUID + ".webp", S3ImageStorage.thumbnailKeyOf("chat/12/" + UUID + ".webp"));
    }

    @Test
    void thumbnailReadSignsThumbnailKey() {
        when(objectStorage.presignDownload("thumbnails/chat/12/" + UUID + ".webp"))
                .thenReturn("https://example.com/signed-thumbnail");

        assertEquals("https://example.com/signed-thumbnail", images.presignThumbnailRead("chat/12/" + UUID + ".jpg"));
    }
}
