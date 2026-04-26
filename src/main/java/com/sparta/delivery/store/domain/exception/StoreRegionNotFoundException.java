package com.sparta.delivery.store.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class StoreRegionNotFoundException extends BaseException {

    public StoreRegionNotFoundException() {
        super(StoreErrorCode.STORE_REGION_NOT_FOUND);
    }
}
