package com.sparta.delivery.order.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class StoreNotFoundException extends BaseException {

    public StoreNotFoundException() {
        super(OrderErrorCode.STORE_NOT_FOUND);
    }
}
