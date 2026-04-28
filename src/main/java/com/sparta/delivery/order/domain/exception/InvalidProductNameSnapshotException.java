package com.sparta.delivery.order.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidProductNameSnapshotException extends BaseException {

    public InvalidProductNameSnapshotException() {
        super(OrderErrorCode.INVALID_PRODUCT_NAME_SNAPSHOT);
    }
}
