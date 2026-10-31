package com.itssum.global.common;

/**
 * 전화번호 입력 규칙과 저장 형식. 하이픈은 없어도 되고, 저장할 때 위치를 맞춘다.
 */
public final class PhoneNumbers {

    /** 휴대폰 (기부자). 010-1234-5678, 01012345678, 011-123-4567 */
    public static final String MOBILE_REGEX = "^01[016789]-?\\d{3,4}-?\\d{4}$";

    /** 기관 연락처. 지역번호(02 포함)·휴대폰·15xx 같은 대표번호 */
    public static final String CONTACT_REGEX = "^(0\\d{1,2}-?\\d{3,4}-?\\d{4}|1\\d{3}-?\\d{4})$";

    private PhoneNumbers() {
    }

    /**
     * 휴대폰 01012345678 → 010-1234-5678, 10자리는 011-123-4567.
     * 서울 지역번호 02 는 두 자리로 자르고 (02-123-4567), 15xx 같은 대표번호 8자리는 1588-1234.
     */
    public static String format(String phone) {
        String digits = phone.replaceAll("\\D", "");
        int lastPartStart = digits.length() - 4;
        if (digits.length() == 8) {
            return digits.substring(0, lastPartStart) + "-" + digits.substring(lastPartStart);
        }
        int areaCodeLength = digits.startsWith("02") ? 2 : 3;
        return digits.substring(0, areaCodeLength) + "-" + digits.substring(areaCodeLength, lastPartStart)
                + "-" + digits.substring(lastPartStart);
    }
}
