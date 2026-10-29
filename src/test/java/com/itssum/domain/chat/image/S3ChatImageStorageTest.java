package com.itssum.domain.chat.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import com.itssum.global.storage.S3ObjectStorage;
import com.itssum.global.storage.S3ObjectStorage.StoredObjectMetadata;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class S3ChatImageStorageTest {

    private final S3ObjectStorage storage = mock(S3ObjectStorage.class);
    private final S3ChatImageStorage images = new S3ChatImageStorage(storage);

    @Test
    void uploadKeyIsScopedToChatRoom() {
        when(storage.presignUpload(anyString(), eq("image/png"))).thenReturn("https://example.com/upload");

        PresignedChatImageUpload result = images.presignUpload(42L, "image/png");

        assertEquals("https://example.com/upload", result.uploadUrl());
        assertEquals("image/png", result.requiredHeaders().get("Content-Type"));
        assertEquals(true, result.objectKey().matches("chat/42/[0-9a-f-]{36}\\.png"));
    }

    @Test
    void anotherRoomImageCannotBeSent() {
        String key = "chat/43/11111111-1111-1111-1111-111111111111.jpg";

        BusinessException error = assertThrows(BusinessException.class,
                () -> images.verifyUploaded(42L, key));

        assertEquals(ErrorCode.CHAT_IMAGE_NOT_UPLOADED, error.getErrorCode());
    }

    @Test
    void uploadedImageMustBeWithinSizeLimit() {
        String key = "chat/42/11111111-1111-1111-1111-111111111111.jpg";
        when(storage.find(key)).thenReturn(Optional.of(new StoredObjectMetadata(11L * 1024 * 1024, "image/jpeg")));

        BusinessException error = assertThrows(BusinessException.class,
                () -> images.verifyUploaded(42L, key));

        assertEquals(ErrorCode.CHAT_IMAGE_NOT_UPLOADED, error.getErrorCode());
        verify(storage).find(key);
    }
}
