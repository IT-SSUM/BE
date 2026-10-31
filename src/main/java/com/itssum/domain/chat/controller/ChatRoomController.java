package com.itssum.domain.chat.controller;

import com.itssum.domain.chat.dto.request.MarkChatReadRequest;
import com.itssum.domain.chat.dto.response.ChatMessagePageResponse;
import com.itssum.domain.chat.dto.response.ChatRoomResponse;
import com.itssum.domain.chat.service.ChatQueryService;
import com.itssum.domain.chat.service.ChatUserResolver;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "채팅", description = "채팅방·채팅 메시지 API")
@RestController
@RequestMapping("/api/chat-rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatUserResolver userResolver;
    private final ChatQueryService queryService;

    @Operation(summary = "채팅방 목록 조회",
            description = "참여 중인 채팅방을 최근 메시지 순으로 반환합니다. 상대방·연결된 물품·매칭 상태·안 읽은 메시지 수를 포함합니다.")
    @GetMapping
    public ApiResponse<List<ChatRoomResponse>> list(Principal principal) {
        return ApiResponse.ok(SuccessCode.CHAT_ROOMS_FOUND,
                queryService.list(userResolver.requireUser(principal).getId()));
    }

    @Operation(summary = "채팅 메시지 내역 조회",
            description = "참여 중인 채팅방의 메시지를 최신순으로 조회합니다. 다음 내역은 nextCursor를 beforeId로 전달합니다. 이미지 content는 임시 조회 URL입니다.")
    @GetMapping("/{chatRoomId}/messages")
    public ApiResponse<ChatMessagePageResponse> messages(
            @Parameter(description = "채팅방 ID", example = "12") @PathVariable Long chatRoomId,
            @Parameter(description = "이 ID보다 이전 메시지를 조회합니다", example = "105")
            @RequestParam(required = false) Long beforeId,
            @Parameter(description = "조회 개수(1~100, 기본 30)", example = "30")
            @RequestParam(defaultValue = "30") int size,
                                                          Principal principal) {
        return ApiResponse.ok(SuccessCode.CHAT_MESSAGES_FOUND, queryService.messages(chatRoomId,
                userResolver.requireUser(principal).getId(), beforeId, size));
    }

    @Operation(summary = "채팅 메시지 읽음 처리",
            description = "마지막으로 읽은 메시지 ID를 저장합니다. 읽음 위치가 앞으로 이동하면 /topic/chat-rooms/{chatRoomId}/read 구독자에게 변경을 전송합니다.")
    @PatchMapping("/{chatRoomId}/read")
    public ApiResponse<Void> markRead(
            @Parameter(description = "채팅방 ID", example = "12") @PathVariable Long chatRoomId,
                                      @Valid @RequestBody MarkChatReadRequest request,
                                      Principal principal) {
        queryService.markRead(chatRoomId, userResolver.requireUser(principal).getId(), request.lastReadMessageId());
        return ApiResponse.ok(SuccessCode.CHAT_READ_MARKED);
    }
}
