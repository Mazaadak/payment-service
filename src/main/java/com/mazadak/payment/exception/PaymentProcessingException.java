package com.mazadak.payment.exception;

import lombok.Getter;

@Getter
public class PaymentProcessingException extends RuntimeException {
    private final String orderId;

    public PaymentProcessingException(String message, String orderId) {
        super(message);
        this.orderId = orderId;
    }

}