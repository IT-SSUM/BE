package com.itssum.domain.item.repository;

import com.itssum.domain.item.entity.NeededItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NeededItemRepository extends JpaRepository<NeededItem, Long> {
}
