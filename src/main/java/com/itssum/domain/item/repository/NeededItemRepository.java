package com.itssum.domain.item.repository;

import com.itssum.domain.item.entity.NeededItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface NeededItemRepository extends JpaRepository<NeededItem, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from NeededItem n where n.id = :id")
    Optional<NeededItem> findByIdForUpdate(@Param("id") Long id);
}
