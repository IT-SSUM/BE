package com.itssum.domain.item.repository;

import com.itssum.domain.item.entity.ItemCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemCategoryRepository extends JpaRepository<ItemCategory, Long> {

    List<ItemCategory> findAllByOrderBySortOrderAscIdAsc();
}
