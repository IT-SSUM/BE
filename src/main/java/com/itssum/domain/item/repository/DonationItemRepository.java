package com.itssum.domain.item.repository;

import com.itssum.domain.item.entity.ConditionGrade;
import com.itssum.domain.item.entity.DonationItem;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
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

    /**
     * 기부 물품 탐색: 삭제되지 않은 모집중 물품만, 최신 등록순. 조건이 null 이면 걸지 않는다.
     * likePattern 은 ItemSearchCondition.likePattern() 값이다 (ESCAPE '!').
     */
    @Query("select d from DonationItem d join fetch d.donor dn join fetch d.region join fetch d.category "
            + "where d.deletedAt is null and d.status = com.itssum.domain.item.entity.DonationItemStatus.RECRUITING "
            + "and (:beforeId is null or d.id < :beforeId) "
            + "and (:categoryId is null or d.category.id = :categoryId) "
            + "and (:regionId is null or d.region.id = :regionId) "
            + "and (:condition is null or d.conditionGrade = :condition) "
            + "and (:likePattern is null or d.itemName like :likePattern escape '!' "
            + "or dn.name like :likePattern escape '!') "
            + "order by d.id desc")
    List<DonationItem> search(@Param("likePattern") String likePattern, @Param("categoryId") Long categoryId,
                              @Param("regionId") Long regionId, @Param("condition") ConditionGrade condition,
                              @Param("beforeId") Long beforeId, Pageable pageable);

    /** 기부자 「등록한 물품」: 상태와 관계없이 삭제되지 않은 내 물품, 최신 등록순. */
    @Query("select d from DonationItem d join fetch d.category "
            + "where d.donor.id = :donorId and d.deletedAt is null "
            + "and (:categoryId is null or d.category.id = :categoryId) "
            + "and (:beforeId is null or d.id < :beforeId) "
            + "order by d.id desc")
    List<DonationItem> findMine(@Param("donorId") Long donorId, @Param("categoryId") Long categoryId,
                                @Param("beforeId") Long beforeId, Pageable pageable);
}
