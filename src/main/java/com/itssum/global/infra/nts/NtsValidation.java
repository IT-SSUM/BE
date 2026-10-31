package com.itssum.global.infra.nts;

import java.time.LocalDate;

/**
 * 진위확인 결과. 국세청 코드 원문을 그대로 담는다. 정보가 일치할 때만 상태(businessStatusCode 등)가 온다.
 *
 * @param validCode          valid: 01 일치 | 02 불일치
 * @param businessStatusCode b_stt_cd: 01 계속 | 02 휴업 | 03 폐업
 * @param taxType            tax_type (과세유형 메시지)
 * @param closedDate         end_dt (폐업일)
 */
public record NtsValidation(String validCode, String businessStatusCode, String taxType, LocalDate closedDate) {

    public boolean matched() {
        return "01".equals(validCode);
    }

    /** 고유번호 단체는 상태 코드가 비어 올 수 있어서, 휴업·폐업으로 명시된 경우만 본다. */
    public boolean suspendedOrClosed() {
        return "02".equals(businessStatusCode) || "03".equals(businessStatusCode);
    }
}
