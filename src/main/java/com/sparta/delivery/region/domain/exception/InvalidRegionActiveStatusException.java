package com.sparta.delivery.region.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidRegionActiveStatusException extends BaseException {

    public InvalidRegionActiveStatusException() {
        super(RegionErrorCode.INVALID_REGION_ACTIVE_STATUS);
    }
}
