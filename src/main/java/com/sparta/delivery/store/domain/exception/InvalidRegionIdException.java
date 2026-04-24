package com.sparta.delivery.store.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidRegionIdException extends BaseException {

    public InvalidRegionIdException() {
        super(StoreErrorCode.INVALID_REGION_ID);
    }
}
