package com.sparta.delivery.order.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class InvalidDeliveryAddressSnapshotException extends BaseException {

    public InvalidDeliveryAddressSnapshotException() {
        super(OrderErrorCode.INVALID_DELIVERY_ADDRESS_SNAPSHOT);
    }
}
