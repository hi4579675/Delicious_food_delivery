package com.sparta.delivery.region.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidParentRegionException extends BaseException {

    public InvalidParentRegionException() {
        super(RegionErrorCode.INVALID_PARENT_REGION);
    }
}
