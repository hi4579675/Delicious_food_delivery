package com.sparta.delivery.region.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidRegionDepthException extends BaseException {

    public InvalidRegionDepthException() {
        super(RegionErrorCode.INVALID_REGION_DEPTH);
    }
}
