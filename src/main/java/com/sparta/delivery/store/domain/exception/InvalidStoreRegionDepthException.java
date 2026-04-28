package com.sparta.delivery.store.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidStoreRegionDepthException extends BaseException {

    public InvalidStoreRegionDepthException() {
        super(StoreErrorCode.INVALID_STORE_REGION_DEPTH);
    }
}
