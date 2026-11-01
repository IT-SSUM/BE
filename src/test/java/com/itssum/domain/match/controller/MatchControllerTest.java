package com.itssum.domain.match.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itssum.domain.match.dto.response.MatchAcceptedResponse;
import com.itssum.domain.match.dto.response.MatchCreatedResponse;
import com.itssum.domain.match.dto.response.MatchResponse;
import com.itssum.domain.item.entity.ConditionGrade;
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
                9L, 12L, "도서", 25L, "도서", 7L, 8L, RequesterType.DONOR,
                3, "도서를 기부합니다", MatchStatus.PENDING, LocalDateTime.of(2026, 10, 31, 15, 30)));

        mockMvc.perform(post("/api/matches")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.issue(7L, UserRole.DONOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"donationItemId": 12, "neededItemId": 25, "quantity": 3}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("MATCH_201"))
                .andExpect(jsonPath("$.data.matchId").value(9))
                .andExpect(jsonPath("$.data.quantity").value(3))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.createdAt").value("2026-10-31T15:30:00"))
                .andExpect(jsonPath("$.data.deliveryMethod").doesNotExist());
    }

    @Test
    void acceptanceReturnsChatRoomIdForNavigation() throws Exception {
        User user = mock(User.class);
        when(user.isActive()).thenReturn(true);
        when(users.findById(8L)).thenReturn(Optional.of(user));
        when(matchService.accept(9L, user)).thenReturn(new MatchAcceptedResponse(
                9L, 33L, 12L, "도서", 25L, "도서", 7L, 8L, RequesterType.DONOR,
                3, "도서를 기부합니다", MatchStatus.TRADING,
                LocalDateTime.of(2026, 10, 31, 15, 30), LocalDateTime.of(2026, 10, 31, 15, 35)));

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
    void matchListUsesJwtSubjectAndMatchSuccessCode() throws Exception {
        User user = mock(User.class);
        when(user.isActive()).thenReturn(true);
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(matchService.list(user)).thenReturn(List.of(new MatchResponse(
                9L, 12L, "도서", 25L, "도서", 7L, 8L, RequesterType.DONOR,
                3, "도서 기부", MatchStatus.PENDING, null, null, null, null,
                LocalDateTime.of(2026, 10, 31, 15, 30), null, null, null, null, null,
                3L, "도서·문구", "권", ConditionGrade.HIGH, "경기 부천시", 5,
                "경기 부천시", 2, "기관 입구", "최윤서", "기관 A", "경기 부천시 길 12", null)));

        mockMvc.perform(get("/api/matches")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.issue(7L, UserRole.DONOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MATCH_200"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].matchId").value(9))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data[0].donorName").value("최윤서"))
                .andExpect(jsonPath("$.data[0].unit").value("권"))
                .andExpect(jsonPath("$.data[0].neededItemAdditionalQuantity").value(2))
                .andExpect(jsonPath("$.data[0].deliveryMethod").doesNotExist());
    }
}
