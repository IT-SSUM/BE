package com.itssum.domain.match.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itssum.domain.match.dto.response.MatchAcceptedResponse;
import com.itssum.domain.match.dto.response.MatchAppointmentResponse;
import com.itssum.domain.match.dto.response.MatchCreatedResponse;
import com.itssum.domain.match.dto.response.MatchListResponse;
import com.itssum.domain.match.dto.response.MatchQuantityResponse;
import com.itssum.domain.match.dto.response.MatchReservedResponse;
import com.itssum.domain.match.dto.response.MatchWithdrawnResponse;
import com.itssum.domain.match.entity.DeliveryMethod;
import com.itssum.domain.match.entity.MatchStatus;
import com.itssum.domain.match.entity.RequesterType;
import com.itssum.domain.match.service.MatchService;
import com.itssum.domain.match.service.MatchUserResolver;
import com.itssum.domain.user.entity.User;
import com.itssum.domain.user.entity.UserRole;
import com.itssum.domain.user.repository.UserRepository;
import com.itssum.global.security.AccessTokenProvider;
import com.itssum.support.SecurityTestConfig;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MatchController.class)
@Import(MatchUserResolver.class)
@SecurityTestConfig
class MatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccessTokenProvider tokens;

    @MockitoBean
    private MatchService matchService;

    @MockitoBean
    private UserRepository users;

    @Test
    void matchCreationUsesAuthenticatedUserAndCreatedStatus() throws Exception {
        User user = mock(User.class);
        when(user.isActive()).thenReturn(true);
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(matchService.create(eq(user), any())).thenReturn(new MatchCreatedResponse(
                9L, MatchStatus.PENDING, LocalDateTime.of(2026, 10, 31, 15, 30)));

        mockMvc.perform(post("/api/matches")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.issue(7L, UserRole.DONOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"donationItemId": 12, "neededItemId": 25, "quantity": 3}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("MATCH_201"))
                .andExpect(jsonPath("$.data.matchId").value(9))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.createdAt").value("2026-10-31T15:30:00"))
                .andExpect(jsonPath("$.data.quantity").doesNotExist());
    }

    @Test
    void acceptanceReturnsChatRoomIdForNavigation() throws Exception {
        User user = mock(User.class);
        when(user.isActive()).thenReturn(true);
        when(users.findById(8L)).thenReturn(Optional.of(user));
        when(matchService.accept(9L, user)).thenReturn(new MatchAcceptedResponse(
                9L, 33L, MatchStatus.TRADING, LocalDateTime.of(2026, 10, 31, 15, 35)));

        mockMvc.perform(patch("/api/matches/9/accept")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.issue(8L, UserRole.ORGANIZATION)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MATCH_200_6"))
                .andExpect(jsonPath("$.data.chatRoomId").value(33))
                .andExpect(jsonPath("$.data.status").value("TRADING"))
                .andExpect(jsonPath("$.data.respondedAt").value("2026-10-31T15:35:00"))
                .andExpect(jsonPath("$.data.reservedAt").doesNotExist());
    }

    @Test
    void requesterCanWithdrawPendingMatchWithoutRequestBody() throws Exception {
        User user = mock(User.class);
        when(user.isActive()).thenReturn(true);
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(matchService.withdraw(9L, user)).thenReturn(new MatchWithdrawnResponse(
                9L, MatchStatus.CANCELED, LocalDateTime.of(2026, 11, 3, 10, 30)));

        mockMvc.perform(patch("/api/matches/9/withdraw")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.issue(7L, UserRole.DONOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MATCH_200_8"))
                .andExpect(jsonPath("$.data.matchId").value(9))
                .andExpect(jsonPath("$.data.status").value("CANCELED"))
                .andExpect(jsonPath("$.data.canceledAt").value("2026-11-03T10:30:00"))
                .andExpect(jsonPath("$.data.donationItemId").doesNotExist())
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());

        verify(matchService).withdraw(9L, user);
    }

    @Test
    void matchListUsesJwtSubjectAndMatchSuccessCode() throws Exception {
        User user = mock(User.class);
        when(user.isActive()).thenReturn(true);
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(matchService.list(user)).thenReturn(List.of(new MatchListResponse(
                9L, RequesterType.DONOR, MatchStatus.PENDING, "도서", "도서", "최윤서", "기관 A",
                3, "권", 3L, "경기 부천시", "경기 부천시", null, null, null)));

        mockMvc.perform(get("/api/matches")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.issue(7L, UserRole.DONOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MATCH_200"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].matchId").value(9))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data[0].donorName").value("최윤서"))
                .andExpect(jsonPath("$.data[0].unit").value("권"))
                .andExpect(jsonPath("$.data[0].itemCategoryId").value(3))
                .andExpect(jsonPath("$.data[0].neededItemAdditionalQuantity").doesNotExist())
                .andExpect(jsonPath("$.data[0].deliveryMethod").doesNotExist());
    }

    @Test
    void reservationReturnsOnlyItsChangedState() throws Exception {
        User user = mock(User.class);
        when(user.isActive()).thenReturn(true);
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(matchService.reserve(9L, user)).thenReturn(new MatchReservedResponse(
                9L, MatchStatus.RESERVED, 3, LocalDateTime.of(2026, 11, 3, 11, 0)));

        mockMvc.perform(patch("/api/matches/9/reserve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.issue(7L, UserRole.DONOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantity").value(3))
                .andExpect(jsonPath("$.data.reservedAt").value("2026-11-03T11:00:00"))
                .andExpect(jsonPath("$.data.donationItemName").doesNotExist());
    }

    @Test
    void quantityChangeRequiresPositiveBodyAndReturnsUpdatedQuantity() throws Exception {
        User user = mock(User.class);
        when(user.isActive()).thenReturn(true);
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(matchService.updateQuantity(eq(9L), eq(user), any()))
                .thenReturn(new MatchQuantityResponse(9L, MatchStatus.TRADING, 2, 3));

        mockMvc.perform(patch("/api/matches/9/quantity")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.issue(7L, UserRole.DONOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MATCH_200_9"))
                .andExpect(jsonPath("$.data.matchId").value(9))
                .andExpect(jsonPath("$.data.status").value("TRADING"))
                .andExpect(jsonPath("$.data.quantity").value(2))
                .andExpect(jsonPath("$.data.initialQuantity").value(3))
                .andExpect(jsonPath("$.data.reservedAt").doesNotExist());

        mockMvc.perform(patch("/api/matches/9/quantity")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.issue(7L, UserRole.DONOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void appointmentReturnsValuesShownOnTheAppointmentCard() throws Exception {
        User user = mock(User.class);
        when(user.isActive()).thenReturn(true);
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(matchService.confirmAppointment(eq(9L), eq(user), any())).thenReturn(new MatchAppointmentResponse(
                9L, MatchStatus.RESERVED, DeliveryMethod.DIRECT,
                LocalDateTime.of(2030, 10, 9, 14, 0), "센터 입구", null));

        mockMvc.perform(patch("/api/matches/9/appointment")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.issue(7L, UserRole.DONOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"deliveryMethod":"DIRECT","scheduledAt":"2030-10-09T14:00:00+09:00","deliveryPlace":"센터 입구"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scheduledAt").value("2030-10-09T14:00:00"))
                .andExpect(jsonPath("$.data.deliveryPlace").value("센터 입구"))
                .andExpect(jsonPath("$.data.receiverName").doesNotExist())
                .andExpect(jsonPath("$.data.donorName").doesNotExist());
    }
}
