package com.itssum.domain.item.service;

import com.itssum.domain.item.dto.response.ItemPageResponse;
import com.itssum.global.exception.BusinessException;
import com.itssum.global.exception.ErrorCode;
import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/** 물품 목록의 커서 페이지 (채팅·알림과 같은 방식: beforeId 보다 작은 ID 를 size + 1 개 읽어 다음 페이지 여부를 정한다). */
final class ItemPaging {

    static final int MAX_SIZE = 50;

    private ItemPaging() {
    }

    static Pageable request(Long beforeId, int size) {
        if (size < 1 || size > MAX_SIZE || (beforeId != null && beforeId < 1)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "조회 개수는 1~" + MAX_SIZE + ", beforeId 는 1 이상이어야 합니다.");
        }
        return PageRequest.of(0, size + 1);
    }

    /** 한 개 더 읽은 결과를 잘라 페이지로 만든다. mapper 는 잘린 목록을 한 번에 받는다 (대표 사진을 한 번에 읽도록). */
    static <E, T> ItemPageResponse<T> page(List<E> fetched, int size, Function<E, Long> id,
                                           Function<List<E>, List<T>> mapper) {
        boolean hasNext = fetched.size() > size;
        List<E> selected = fetched.stream().limit(size).toList();
        Long nextCursor = hasNext ? id.apply(selected.getLast()) : null;
        return new ItemPageResponse<>(mapper.apply(selected), nextCursor, hasNext);
    }
}
