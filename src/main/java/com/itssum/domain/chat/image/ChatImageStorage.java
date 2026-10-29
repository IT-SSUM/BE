package com.itssum.domain.chat.image;

public interface ChatImageStorage {

    PresignedChatImageUpload presignUpload(Long chatRoomId, String contentType);

    void verifyUploaded(Long chatRoomId, String objectKey);

    String presignRead(String objectKey);
}
