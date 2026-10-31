package com.itssum.domain.match.controller;

import com.itssum.domain.match.dto.request.CreateMatchRequest;
import com.itssum.domain.match.dto.request.ConfirmAppointmentRequest;
import com.itssum.domain.match.dto.request.MatchReasonRequest;
import com.itssum.domain.match.dto.response.MatchAcceptedResponse;
import com.itssum.domain.match.dto.response.MatchCreatedResponse;
import com.itssum.domain.match.dto.response.MatchRejectedResponse;
import com.itssum.domain.match.dto.response.MatchResponse;
import com.itssum.domain.match.service.MatchService;
import com.itssum.domain.match.service.MatchUserResolver;
import com.itssum.global.common.ApiResponse;
import com.itssum.global.exception.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "매칭", description = "기부 제안·요청 및 거래 상태 관리 API")
@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;
    private final MatchUserResolver userResolver;

    @Operation(summary = "기부 제안·요청 생성", description = "상대방의 응답을 기다리는 제안·요청을 생성합니다. 수량은 확보하지 않습니다.")
    @PostMapping
    public ApiResponse<MatchCreatedResponse> create(@Valid @RequestBody CreateMatchRequest request, Principal principal) {
        return ApiResponse.ok(SuccessCode.MATCH_CREATED,
                matchService.create(userResolver.requireUser(principal), request));
    }

    @Operation(summary = "매칭 목록 조회", description = "보내거나 받은 제안·요청과 개별 거래를 최신순으로 조회합니다.")
    @GetMapping
    public ApiResponse<List<MatchResponse>> list(Principal principal) {
        return ApiResponse.ok(SuccessCode.MATCH_LIST_FOUND,
                matchService.list(userResolver.requireUser(principal)));
    }

    @Operation(summary = "매칭 상세 조회", description = "응답 대기 중인 제안·요청 또는 개별 거래를 조회합니다.")
    @GetMapping("/{matchId}")
    public ApiResponse<MatchResponse> detail(@Parameter(description = "매칭 ID") @PathVariable Long matchId,
                                             Principal principal) {
        return ApiResponse.ok(SuccessCode.MATCH_FOUND,
                matchService.detail(matchId, userResolver.requireUser(principal)));
    }

    @Operation(summary = "기부 제안·요청 수락", description = "받은 제안·요청을 수락해 거래중으로 바꾸고 채팅방을 엽니다. 수량은 아직 확보하지 않습니다.")
    @PatchMapping("/{matchId}/accept")
    public ApiResponse<MatchAcceptedResponse> accept(@PathVariable Long matchId, Principal principal) {
        return ApiResponse.ok(SuccessCode.MATCH_ACCEPTED,
                matchService.accept(matchId, userResolver.requireUser(principal)));
    }

    @Operation(summary = "기부 제안·요청 거절", description = "받은 제안·요청을 거절합니다. 채팅방과 예약 수량은 생성되지 않습니다.")
    @PatchMapping("/{matchId}/reject")
    public ApiResponse<MatchRejectedResponse> reject(@PathVariable Long matchId, Principal principal) {
        return ApiResponse.ok(SuccessCode.MATCH_REJECTED,
                matchService.reject(matchId, userResolver.requireUser(principal)));
    }

    @Operation(summary = "거래 예약", description = "상대 수락 후 거래중인 건을 기부자만 예약중으로 바꾸고 양쪽 물품의 예약 수량을 확보합니다.")
    @PatchMapping("/{matchId}/reserve")
    public ApiResponse<MatchResponse> reserve(@PathVariable Long matchId, Principal principal) {
        return ApiResponse.ok(SuccessCode.MATCH_RESERVED,
                matchService.reserve(matchId, userResolver.requireUser(principal)));
    }

    @Operation(summary = "거래 완료", description = "기관이 실제 수령을 확인한 뒤 예약 수량을 완료 수량으로 옮깁니다.")
    @PatchMapping("/{matchId}/complete")
    public ApiResponse<MatchResponse> complete(@PathVariable Long matchId, Principal principal) {
        return ApiResponse.ok(SuccessCode.MATCH_COMPLETED,
                matchService.complete(matchId, userResolver.requireUser(principal)));
    }

    @Operation(summary = "거래 취소", description = "양측 모두 거래중·예약중 거래를 취소할 수 있습니다. 예약중이었다면 수량을 반환합니다.")
    @PatchMapping("/{matchId}/cancel")
    public ApiResponse<MatchResponse> cancel(@PathVariable Long matchId,
                                             @Valid @RequestBody(required = false) MatchReasonRequest request,
                                             Principal principal) {
        return ApiResponse.ok(SuccessCode.MATCH_CANCELED, matchService.cancel(matchId, userResolver.requireUser(principal),
                request == null ? null : request.reason()));
    }

    @Operation(summary = "전달 약속 확정·변경", description = "예약중 거래의 당사자 누구나 협의한 전달 방법·일정·장소를 저장하거나 변경할 수 있습니다. 저장할 때마다 채팅에 약속 카드가 생성됩니다.")
    @PatchMapping("/{matchId}/appointment")
    public ApiResponse<MatchResponse> confirmAppointment(@PathVariable Long matchId,
            @Valid @RequestBody ConfirmAppointmentRequest request, Principal principal) {
        return ApiResponse.ok(SuccessCode.MATCH_APPOINTMENT_SAVED,
                matchService.confirmAppointment(matchId, userResolver.requireUser(principal), request));
    }
}
