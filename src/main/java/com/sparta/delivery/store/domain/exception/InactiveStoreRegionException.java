package com.sparta.delivery.store.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InactiveStoreRegionException extends BaseException {

    public InactiveStoreRegionException() {
        super(StoreErrorCode.INACTIVE_STORE_REGION);
    }
}
