package com.itssum.global.infra.s3;

import java.util.Map;

/** 이미지 업로드 key, 서명된 PUT URL, 업로드 요청에 그대로 넣어야 하는 헤더. */
public record PresignedImageUpload(String objectKey, String uploadUrl, Map<String, String> requiredHeaders) {
}
