package com.itssum.domain.chat.repository;

import com.itssum.domain.chat.entity.ChatRoom;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByMatch_Id(Long matchId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ChatRoom r where r.id = :roomId")
    Optional<ChatRoom> findByIdForUpdate(@Param("roomId") Long roomId);
}
