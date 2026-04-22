package com.sparta.delivery.region.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidRegionCodeException extends BaseException {

    public InvalidRegionCodeException() {
        super(RegionErrorCode.INVALID_REGION_CODE);
    }
}
