package com.itssum.domain.item.repository;

import com.itssum.domain.item.entity.NeededItem;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NeededItemRepository extends JpaRepository<NeededItem, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from NeededItem n where n.id = :id")
    Optional<NeededItem> findByIdForUpdate(@Param("id") Long id);

    /** 회원 탈퇴 시 그 기관의 필요 물품을 목록에서 내린다 (soft delete). 바꾼 행 수를 돌려준다. */
    @Modifying
    @Query("update NeededItem n set n.deletedAt = :now where n.organization.id = :organizationId and n.deletedAt is null")
    int softDeleteAllByOrganizationId(@Param("organizationId") Long organizationId, @Param("now") LocalDateTime now);
}
