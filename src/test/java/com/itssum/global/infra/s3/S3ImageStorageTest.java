package com.itssum.global.infra.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
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
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class S3ImageStorageTest {

    private static final String UUID = "11111111-1111-1111-1111-111111111111";
    private static final byte[] JPEG_HEAD = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 0, 1};
    private static final byte[] PNG_HEAD = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D};
    private static final byte[] WEBP_HEAD = {'R', 'I', 'F', 'F', 0x24, 0, 0, 0, 'W', 'E', 'B', 'P'};

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
        verify(objectStorage, never()).readHead(anyString(), anyString(), anyInt());
    }

    @Test
    void validUploadPasses() {
        String key = "chat/42/" + UUID + ".jpg";
        when(objectStorage.find(key))
                .thenReturn(Optional.of(new StoredObjectMetadata(1024L * 1024, "image/jpeg", "etag-1")));
        when(objectStorage.readHead(key, "etag-1", 12)).thenReturn(JPEG_HEAD);

        assertTrue(images.verifyUploaded("chat", 42L, key));
        verify(objectStorage, never()).delete(anyString(), anyString());
    }

    @Test
    void pngAndWebpSignaturesPass() {
        String png = "chat/42/" + UUID + ".png";
        String webp = "chat/42/" + UUID + ".webp";
        when(objectStorage.find(png)).thenReturn(Optional.of(new StoredObjectMetadata(1024L, "image/png", "etag-1")));
        when(objectStorage.find(webp)).thenReturn(Optional.of(new StoredObjectMetadata(1024L, "image/webp", "etag-2")));
        when(objectStorage.readHead(png, "etag-1", 12)).thenReturn(PNG_HEAD);
        when(objectStorage.readHead(webp, "etag-2", 12)).thenReturn(WEBP_HEAD);

        assertTrue(images.verifyUploaded("chat", 42L, png));
        assertTrue(images.verifyUploaded("chat", 42L, webp));
    }

    @Test
    void nonImageBytesAreDeleted() {
        String key = "chat/42/" + UUID + ".jpg";
        when(objectStorage.find(key))
                .thenReturn(Optional.of(new StoredObjectMetadata(1024L, "image/jpeg", "etag-1")));
        when(objectStorage.readHead(key, "etag-1", 12))
                .thenReturn("<html><body>".getBytes(StandardCharsets.US_ASCII));

        assertFalse(images.verifyUploaded("chat", 42L, key));
        verify(objectStorage).delete(key, "etag-1");
    }

    @Test
    void signatureMustMatchDeclaredType() {
        String key = "chat/42/" + UUID + ".png";
        when(objectStorage.find(key))
                .thenReturn(Optional.of(new StoredObjectMetadata(1024L, "image/png", "etag-1")));
        when(objectStorage.readHead(key, "etag-1", 12)).thenReturn(JPEG_HEAD);

        assertFalse(images.verifyUploaded("chat", 42L, key));
        verify(objectStorage).delete(key, "etag-1");
    }

    @Test
    void headShorterThanSignatureIsRejected() {
        String key = "chat/42/" + UUID + ".webp";
        when(objectStorage.find(key))
                .thenReturn(Optional.of(new StoredObjectMetadata(4L, "image/webp", "etag-1")));
        when(objectStorage.readHead(key, "etag-1", 12)).thenReturn(new byte[] {'R', 'I', 'F', 'F'});

        assertFalse(images.verifyUploaded("chat", 42L, key));
        verify(objectStorage).delete(key, "etag-1");
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
