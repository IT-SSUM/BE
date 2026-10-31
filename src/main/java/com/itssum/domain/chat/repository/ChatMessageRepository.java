package com.itssum.domain.chat.repository;

import com.itssum.domain.chat.entity.ChatMessage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    @Query("select m from ChatMessage m where m.chatRoom.id = :roomId "
            + "and (:beforeId is null or m.id < :beforeId) order by m.id desc")
    List<ChatMessage> findRecent(@Param("roomId") Long roomId,
                                 @Param("beforeId") Long beforeId,
                                 Pageable pageable);

    Optional<ChatMessage> findByIdAndChatRoom_Id(Long messageId, Long roomId);

    @Query("select count(m) from ChatMessage m where m.chatRoom.id = :roomId "
            + "and m.id > :lastReadId and m.sender is not null and m.sender.id <> :userId")
    long countUnread(@Param("roomId") Long roomId,
                     @Param("lastReadId") Long lastReadId,
                     @Param("userId") Long userId);

    Optional<ChatMessage> findTopByChatRoom_IdOrderByIdDesc(Long roomId);
}
