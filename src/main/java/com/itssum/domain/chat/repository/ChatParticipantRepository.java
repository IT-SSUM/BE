package com.itssum.domain.chat.repository;

import com.itssum.domain.chat.entity.ChatParticipant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {
    boolean existsByChatRoom_IdAndUser_Id(Long chatRoomId, Long userId);
    Optional<ChatParticipant> findByChatRoom_IdAndUser_Id(Long chatRoomId, Long userId);
    Optional<ChatParticipant> findByChatRoom_IdAndUser_IdNot(Long chatRoomId, Long userId);
    List<ChatParticipant> findAllByUser_Id(Long userId);
}
