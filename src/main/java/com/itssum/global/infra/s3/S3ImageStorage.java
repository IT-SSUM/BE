package com.itssum.global.infra.s3;

import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import com.itssum.global.infra.s3.S3ObjectStorage.StoredObjectMetadata;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 이미지 업로드 공통 규칙. key는 {접두사}/{소유자ID}/{uuid}.{확장자}이고,
 * 접두사와 소유자(채팅방, 회원 등)는 사용하는 도메인이 정한다.
 */
@Component
@ConditionalOnS3Bucket
@RequiredArgsConstructor
public class S3ImageStorage {

    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");
    private static final Pattern IMAGE_FILE = Pattern.compile("[0-9a-f-]{36}\\.(jpg|png|webp)");
    private static final Pattern IMAGE_EXTENSION = Pattern.compile("\\.(jpg|png|webp)$");
    // 파일 앞부분의 형식 시그니처. WebP는 RIFF(0~3) + WEBP(8~11)
    private static final int SIGNATURE_BYTES = 12;
    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] RIFF_SIGNATURE = {'R', 'I', 'F', 'F'};
    private static final byte[] WEBP_SIGNATURE = {'W', 'E', 'B', 'P'};

    private final S3ObjectStorage storage;

    public PresignedImageUpload presignUpload(String prefix, Long ownerId, String contentType) {
        if (!isImageType(contentType)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "JPEG, PNG, WebP 이미지만 보낼 수 있습니다.");
        }
        String key = prefix + "/" + ownerId + "/" + UUID.randomUUID() + "." + EXTENSIONS.get(contentType);
        return new PresignedImageUpload(key, storage.presignUpload(key, contentType),
                Map.of("Content-Type", contentType, "If-None-Match", "*"));
    }

    /**
     * 소유자 경로의 key이고 S3에 올라온 객체가 이미지 규칙(크기, Content-Type, 실제 파일 형식)에 맞으면 true.
     * 규칙에 어긋난 객체는 지운다.
     */
    public boolean verifyUploaded(String prefix, Long ownerId, String objectKey) {
        String ownerPath = prefix + "/" + ownerId + "/";
        if (objectKey == null || !objectKey.startsWith(ownerPath)
                || !IMAGE_FILE.matcher(objectKey.substring(ownerPath.length())).matches()) {
            return false;
        }
        Optional<StoredObjectMetadata> found = storage.find(objectKey);
        if (found.isEmpty()) {
            return false;
        }
        StoredObjectMetadata object = found.get();
        if (object.size() == null || object.size() == 0 || object.size() > MAX_IMAGE_BYTES
                || !isImageType(object.contentType())
                || !matchesSignature(object.contentType(), storage.readHead(objectKey, object.eTag(), SIGNATURE_BYTES))) {
            if (object.eTag() != null && !object.eTag().isBlank()) {
                storage.delete(objectKey, object.eTag());
            }
            return false;
        }
        return true;
    }

    public String presignRead(String objectKey) {
        return storage.presignDownload(objectKey);
    }

    /** 썸네일이 아직 없어도 서명만 한다. 열리지 않으면 원본 URL로 대체하는 것은 클라이언트 몫이다. */
    public String presignThumbnailRead(String objectKey) {
        return storage.presignDownload(thumbnailKeyOf(objectKey));
    }

    /** 썸네일 Lambda가 만드는 위치: thumbnails/ 아래 같은 경로, 확장자는 webp. */
    public static String thumbnailKeyOf(String objectKey) {
        return "thumbnails/" + IMAGE_EXTENSION.matcher(objectKey).replaceFirst(".webp");
    }

    private static boolean isImageType(String contentType) {
        return contentType != null && EXTENSIONS.containsKey(contentType);
    }

    /** 선언한 Content-Type과 실제 파일 시그니처가 같은 형식인지 확인한다. */
    private static boolean matchesSignature(String contentType, byte[] head) {
        return switch (contentType) {
            case "image/jpeg" -> startsWith(head, 0, JPEG_SIGNATURE);
            case "image/png" -> startsWith(head, 0, PNG_SIGNATURE);
            case "image/webp" -> startsWith(head, 0, RIFF_SIGNATURE) && startsWith(head, 8, WEBP_SIGNATURE);
            default -> false;
        };
    }

    private static boolean startsWith(byte[] data, int offset, byte[] expected) {
        return data.length >= offset + expected.length
                && Arrays.equals(data, offset, offset + expected.length, expected, 0, expected.length);
    }
}
