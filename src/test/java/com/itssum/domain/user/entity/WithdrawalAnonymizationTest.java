package com.itssum.domain.user.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class WithdrawalAnonymizationTest {

    @Test
    void userWithdrawFreesEmailAndBlocksLogin() {
        User user = User.builder().email("donor@example.com").password("hashed").role(UserRole.DONOR).build();
        ReflectionTestUtils.setField(user, "id", 7L);

        user.withdraw();

        assertEquals("withdrawn-7@itssum.invalid", user.getEmail());
        assertEquals("WITHDRAWN", user.getPassword());
        assertEquals(UserStatus.WITHDRAWN, user.getStatus());
        assertNotNull(user.getDeletedAt());
        assertFalse(user.isActive());
    }

    @Test
    void donorAnonymizeErasesPersonalData() {
        Donor donor = Donor.builder()
                .name("최윤서")
                .birthDate(LocalDate.of(1999, 3, 2))
                .phone("010-1234-5678")
                .introduction("반가워요")
                .build();

        donor.anonymize();

        assertEquals("탈퇴한 회원", donor.getName());
        assertEquals(LocalDate.of(1900, 1, 1), donor.getBirthDate());
        assertEquals("000-0000-0000", donor.getPhone());
        assertNull(donor.getIntroduction());
    }

    @Test
    void organizationAnonymizeFreesBusinessNumber() {
        Organization organization = Organization.builder()
                .name("구로지역아동센터")
                .representativeName("김수현")
                .businessNumber("1234567890")
                .openingDate(LocalDate.of(2015, 3, 2))
                .phone("02-1234-5678")
                .address("서울 구로구 가마산로 12")
                .addressDetail("2층")
                .introduction("돌봄 기관")
                .latitude(new BigDecimal("37.4930158"))
                .longitude(new BigDecimal("126.8868294"))
                .verifiedAt(LocalDateTime.of(2026, 10, 31, 10, 0))
                .build();
        ReflectionTestUtils.setField(organization, "id", 7L);

        organization.anonymize();

        assertEquals("탈퇴한 기관", organization.getName());
        assertEquals("탈퇴", organization.getRepresentativeName());
        assertEquals("000-0000-0000", organization.getPhone());
        assertEquals("탈퇴한 기관", organization.getAddress());
        assertNull(organization.getAddressDetail());
        assertNull(organization.getIntroduction());
        assertNull(organization.getLatitude());
        assertNull(organization.getLongitude());
        assertEquals("W000000007", organization.getBusinessNumber());
    }
}
