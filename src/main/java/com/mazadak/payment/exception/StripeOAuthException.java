package com.mazadak.payment.exception;


import lombok.Getter;

@Getter
public class StripeOAuthException extends RuntimeException {
    private final String sellerId;

    public StripeOAuthException(String message, String sellerId) {
        super(message);
        this.sellerId = sellerId;
    }

    public StripeOAuthException(String message, String sellerId, Throwable cause) {
        super(message, cause);
        this.sellerId = sellerId;
    }

}