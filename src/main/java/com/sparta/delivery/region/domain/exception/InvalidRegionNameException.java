package com.sparta.delivery.region.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidRegionNameException extends BaseException {

    public InvalidRegionNameException() {
        super(RegionErrorCode.INVALID_REGION_NAME);
    }
}
