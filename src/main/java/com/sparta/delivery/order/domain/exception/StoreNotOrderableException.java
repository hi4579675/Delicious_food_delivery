package com.sparta.delivery.order.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class StoreNotOrderableException extends BaseException {

    public StoreNotOrderableException() {
        super(OrderErrorCode.STORE_NOT_ORDERABLE);
    }
}
