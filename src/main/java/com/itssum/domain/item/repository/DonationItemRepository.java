package com.itssum.domain.item.repository;

import com.itssum.domain.item.entity.DonationItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface DonationItemRepository extends JpaRepository<DonationItem, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DonationItem d where d.id = :id")
    Optional<DonationItem> findByIdForUpdate(@Param("id") Long id);
}
