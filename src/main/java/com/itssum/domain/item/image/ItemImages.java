package com.itssum.domain.item.image;

import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import com.itssum.global.infra.s3.PresignedImageUpload;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * 물품 사진 저장소 진입점. 버킷이 설정되지 않은 환경(로컬·테스트)에서는 저장소 빈이 없으므로,
 * 업로드 URL 발급·업로드 확인은 503 으로 막고 조회 URL 은 null 로 준다 (조회 화면은 사진 없이도 열린다).
 */
@Component
@RequiredArgsConstructor
public class ItemImages {

    private final ObjectProvider<ItemImageStorage> storageProvider;

    public PresignedImageUpload presignUpload(ItemImageKind kind, Long ownerId, String contentType) {
        return requireStorage().presignUpload(kind, ownerId, contentType);
    }

    /** 새로 붙이는 사진 key 만 넘긴다. 비어 있으면 저장소를 찾지 않는다. */
    public void verifyUploaded(ItemImageKind kind, Long ownerId, Collection<String> objectKeys) {
        if (objectKeys.isEmpty()) {
            return;
        }
        ItemImageStorage storage = requireStorage();
        for (String objectKey : objectKeys) {
            if (!storage.isUploaded(kind, ownerId, objectKey)) {
                throw new BusinessException(ErrorCode.ITEM_IMAGE_NOT_UPLOADED);
            }
        }
    }

    public String readUrl(String objectKey) {
        if (objectKey == null) {
            return null;
        }
        ItemImageStorage storage = storageProvider.getIfAvailable();
        return storage == null ? null : storage.presignRead(objectKey);
    }

    private ItemImageStorage requireStorage() {
        ItemImageStorage storage = storageProvider.getIfAvailable();
        if (storage == null) {
            throw new BusinessException(ErrorCode.ITEM_IMAGE_STORAGE_UNAVAILABLE);
        }
        return storage;
    }
}
