package com.itssum.domain.item.repository;

import com.itssum.domain.item.entity.DonationItemImage;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DonationItemImageRepository extends JpaRepository<DonationItemImage, Long> {

    /** 목록의 대표 사진(sortOrder 0)을 물품 ID 로 한 번에 읽는다. */
    List<DonationItemImage> findByDonationItemIdInAndSortOrder(Collection<Long> donationItemIds, int sortOrder);
}
