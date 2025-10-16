package com.mazadak.payment.exception;

import lombok.Getter;

@Getter
public class PaymentProcessingException extends RuntimeException {

    public PaymentProcessingException(String message) {
        super(message);
    }

}