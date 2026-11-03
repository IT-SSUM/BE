package com.itssum.domain.item.image;

import com.itssum.global.infra.s3.ConditionalOnS3Bucket;
import com.itssum.global.infra.s3.PresignedImageUpload;
import com.itssum.global.infra.s3.S3ImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 물품 사진은 올린 회원 단위 key(donation-items/{기부자ID}/..., needed-items/{기관ID}/...)를 쓴다. 형식·크기 검사와 S3 처리는 global.infra.s3에 맡긴다. */
@Component
@ConditionalOnS3Bucket
@RequiredArgsConstructor
public class S3ItemImageStorage implements ItemImageStorage {

    private final S3ImageStorage images;

    @Override
    public PresignedImageUpload presignUpload(ItemImageKind kind, Long ownerId, String contentType) {
        return images.presignUpload(kind.prefix(), ownerId, contentType);
    }

    @Override
    public boolean isUploaded(ItemImageKind kind, Long ownerId, String objectKey) {
        return images.verifyUploaded(kind.prefix(), ownerId, objectKey);
    }

    @Override
    public String presignRead(String objectKey) {
        return images.presignRead(objectKey);
    }
}
