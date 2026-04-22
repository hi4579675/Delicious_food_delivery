package com.sparta.delivery.region.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class DuplicateRegionCodeException extends BaseException {

    public DuplicateRegionCodeException() {
        super(RegionErrorCode.DUPLICATE_REGION_CODE);
    }
}
