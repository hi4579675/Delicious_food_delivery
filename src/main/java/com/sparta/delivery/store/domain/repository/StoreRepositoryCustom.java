package com.sparta.delivery.store.domain.repository;

import com.sparta.delivery.store.domain.entity.Store;
import com.sparta.delivery.store.presentation.dto.StoreSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StoreRepositoryCustom {

    Page<Store> searchStores(StoreSearchCondition condition, Pageable pageable);
}
