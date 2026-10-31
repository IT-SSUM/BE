package com.itssum.domain.item.repository;

import com.itssum.domain.item.entity.DonationItem;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DonationItemRepository extends JpaRepository<DonationItem, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DonationItem d where d.id = :id")
    Optional<DonationItem> findByIdForUpdate(@Param("id") Long id);

    /** 회원 탈퇴 시 그 기부자의 물품을 목록에서 내린다 (soft delete). 바꾼 행 수를 돌려준다. */
    @Modifying
    @Query("update DonationItem d set d.deletedAt = :now where d.donor.id = :donorId and d.deletedAt is null")
    int softDeleteAllByDonorId(@Param("donorId") Long donorId, @Param("now") LocalDateTime now);
}
