package com.itssum.domain.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.itssum.domain.user.dto.request.UpdateOrganizationProfileRequest;
import com.itssum.domain.user.dto.response.OrganizationProfileResponse;
import com.itssum.domain.user.entity.Organization;
import com.itssum.domain.user.entity.User;
import com.itssum.domain.user.entity.UserRole;
import com.itssum.domain.user.entity.VerificationStatus;
import com.itssum.global.infra.kakao.GeoPoint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class OrganizationProfileServiceTest {

    private static final String ADDRESS = "서울 구로구 가마산로 12";
    private static final String NEW_ADDRESS = "경기 부천시 길주로 210";
    private static final GeoPoint OLD_POINT = new GeoPoint(new BigDecimal("37.4930158"), new BigDecimal("126.8868294"));
    private static final GeoPoint NEW_POINT = new GeoPoint(new BigDecimal("37.5034138"), new BigDecimal("126.7660309"));

    private final MemberReader memberReader = mock(MemberReader.class);
    private final AddressGeocoder addressGeocoder = mock(AddressGeocoder.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final OrganizationProfileService service = new OrganizationProfileService(memberReader, addressGeocoder,
            new TransactionTemplate(transactionManager));

    private Organization organization;

    @BeforeEach
    void setUp() {
        User user = User.builder().email("org@example.com").password("hashed").role(UserRole.ORGANIZATION).build();
        organization = Organization.builder()
                .user(user)
                .name("구로지역아동센터")
                .representativeName("김수현")
                .businessNumber("1234567890")
                .openingDate(LocalDate.of(2015, 3, 2))
                .phone("02-1234-5678")
                .address(ADDRESS)
                .addressDetail("2층")
                .latitude(OLD_POINT.latitude())
                .longitude(OLD_POINT.longitude())
                .verifiedAt(LocalDateTime.of(2026, 10, 31, 10, 0))
                .build();
        when(memberReader.activeOrganization(2L)).thenReturn(organization);
    }

    @Test
    void getMineReturnsProfile() {
        assertEquals(new OrganizationProfileResponse("org@example.com", "구로지역아동센터", "김수현", "1234567890",
                LocalDate.of(2015, 3, 2), "02-1234-5678", ADDRESS, "2층", VerificationStatus.VERIFIED),
                service.getMine(2L));
    }

    @Test
    void updateMineGeocodesChangedAddressOutsideTransaction() {
        when(addressGeocoder.locate(NEW_ADDRESS)).thenReturn(Optional.of(NEW_POINT));

        OrganizationProfileResponse response = service.updateMine(2L,
                new UpdateOrganizationProfileRequest(null, null, " " + NEW_ADDRESS + " ", "3층"));

        // 카카오 호출 동안 DB 트랜잭션을 잡지 않는다
        InOrder order = inOrder(addressGeocoder, transactionManager);
        order.verify(addressGeocoder).locate(NEW_ADDRESS);
        order.verify(transactionManager).getTransaction(any());
        assertEquals(NEW_ADDRESS, organization.getAddress());
        assertEquals("3층", organization.getAddressDetail());
        assertEquals(NEW_POINT.latitude(), organization.getLatitude());
        assertEquals(NEW_POINT.longitude(), organization.getLongitude());
        assertEquals(NEW_ADDRESS, response.address());
    }

    @Test
    void updateMineClearsCoordinatesWhenGeocodingFails() {
        when(addressGeocoder.locate(NEW_ADDRESS)).thenReturn(Optional.empty());

        service.updateMine(2L, new UpdateOrganizationProfileRequest(null, null, NEW_ADDRESS, null));

        assertEquals(NEW_ADDRESS, organization.getAddress());
        assertNull(organization.getAddressDetail());
        assertNull(organization.getLatitude());
        assertNull(organization.getLongitude());
    }

    @Test
    void updateMineKeepsCoordinatesWhenAddressUnchanged() {
        service.updateMine(2L, new UpdateOrganizationProfileRequest(null, null, ADDRESS, "3층"));

        verifyNoInteractions(addressGeocoder);
        assertEquals(ADDRESS, organization.getAddress());
        assertEquals("3층", organization.getAddressDetail());
        assertEquals(OLD_POINT.latitude(), organization.getLatitude());
        assertEquals(OLD_POINT.longitude(), organization.getLongitude());
    }

    @Test
    void updateMineClearsDetailWithBlank() {
        service.updateMine(2L, new UpdateOrganizationProfileRequest(null, null, null, ""));

        verifyNoInteractions(addressGeocoder);
        assertEquals(ADDRESS, organization.getAddress());
        assertNull(organization.getAddressDetail());
    }

    @Test
    void updateMineChangesNameAndFormatsPhone() {
        service.updateMine(2L, new UpdateOrganizationProfileRequest(" 구로행복센터 ", "0311234567", null, null));

        assertEquals("구로행복센터", organization.getName());
        assertEquals("031-123-4567", organization.getPhone());
        assertEquals("2층", organization.getAddressDetail());
        assertEquals("김수현", organization.getRepresentativeName());
    }
}
