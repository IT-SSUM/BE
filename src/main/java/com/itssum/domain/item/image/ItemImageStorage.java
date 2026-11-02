package com.itssum.domain.item.image;

import com.itssum.global.infra.s3.PresignedImageUpload;

public interface ItemImageStorage {

    PresignedImageUpload presignUpload(ItemImageKind kind, Long ownerId, String contentType);

    /** 올린 회원 경로의 key 이고 S3에 이미지 규칙대로 올라와 있으면 true. */
    boolean isUploaded(ItemImageKind kind, Long ownerId, String objectKey);

    String presignRead(String objectKey);
}
