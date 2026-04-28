package com.sparta.delivery.region.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class RegionNotFoundException extends BaseException {

    public RegionNotFoundException() {
        super(RegionErrorCode.REGION_NOT_FOUND);
    }
}
