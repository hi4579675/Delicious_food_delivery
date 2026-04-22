package com.sparta.delivery.region.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class RegionHasChildrenException extends BaseException {

    public RegionHasChildrenException() {
        super(RegionErrorCode.REGION_HAS_CHILDREN);
    }
}
