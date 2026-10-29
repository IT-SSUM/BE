package com.itssum.domain.chat.repository;

import com.itssum.domain.chat.entity.ChatParticipant;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {
    boolean existsByChatRoom_IdAndUser_Id(Long chatRoomId, Long userId);
    Optional<ChatParticipant> findByChatRoom_IdAndUser_Id(Long chatRoomId, Long userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ChatParticipant p where p.chatRoom.id = :chatRoomId and p.user.id = :userId")
    Optional<ChatParticipant> findForUpdate(@Param("chatRoomId") Long chatRoomId,
                                            @Param("userId") Long userId);
    Optional<ChatParticipant> findByChatRoom_IdAndUser_IdNot(Long chatRoomId, Long userId);
    List<ChatParticipant> findAllByUser_Id(Long userId);
}
