package com.itssum.domain.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.itssum.domain.item.entity.Region;
import com.itssum.domain.user.dto.request.UpdateDonorProfileRequest;
import com.itssum.domain.user.dto.response.DonorProfileResponse;
import com.itssum.domain.user.entity.Donor;
import com.itssum.domain.user.entity.User;
import com.itssum.domain.user.entity.UserRole;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DonorProfileServiceTest {

    private static final LocalDate BIRTH_DATE = LocalDate.of(1999, 3, 2);

    private final MemberReader memberReader = mock(MemberReader.class);
    private final DonorProfileService service = new DonorProfileService(memberReader);

    @Test
    void getReturnsProfileWithRegionName() {
        when(memberReader.activeDonor(1L)).thenReturn(donor());

        assertEquals(new DonorProfileResponse("donor@example.com", "최윤서", BIRTH_DATE, "010-1234-5678", 1L,
                "경기 부천시"), service.get(1L));
    }

    @Test
    void updateChangesOnlyGivenFieldsAndFormatsPhone() {
        Donor donor = donor();
        when(memberReader.activeDonor(1L)).thenReturn(donor);

        DonorProfileResponse response = service.update(1L, new UpdateDonorProfileRequest(null, null, "01099998888"));

        assertEquals("최윤서", donor.getName());
        assertEquals(BIRTH_DATE, donor.getBirthDate());
        assertEquals("010-9999-8888", donor.getPhone());
        assertEquals("010-9999-8888", response.phone());
    }

    @Test
    void updateChangesNameAndBirthDate() {
        Donor donor = donor();
        when(memberReader.activeDonor(1L)).thenReturn(donor);

        service.update(1L, new UpdateDonorProfileRequest(" 최윤아 ", LocalDate.of(2000, 1, 1), null));

        assertEquals("최윤아", donor.getName());
        assertEquals(LocalDate.of(2000, 1, 1), donor.getBirthDate());
        assertEquals("010-1234-5678", donor.getPhone());
    }

    private static Donor donor() {
        User user = User.builder().email("donor@example.com").password("hashed").role(UserRole.DONOR).build();
        Region region = Region.builder().sido("경기").sigungu("부천시").build();
        ReflectionTestUtils.setField(region, "id", 1L);
        return Donor.builder()
                .user(user)
                .region(region)
                .name("최윤서")
                .birthDate(BIRTH_DATE)
                .phone("010-1234-5678")
                .build();
    }
}
