package com.sparta.delivery.payment.domain.exception;

import com.sparta.delivery.common.exception.BaseException;

public class OrderTotalPriceMismatchException extends BaseException {

    public OrderTotalPriceMismatchException() { super(PaymentErrorCode.ORDER_TOTAL_PRICE_MISMATCH); }

}
