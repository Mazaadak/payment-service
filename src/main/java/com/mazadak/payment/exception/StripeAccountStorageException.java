package com.mazadak.payment.exception;

import lombok.Getter;

@Getter
public class StripeAccountStorageException extends RuntimeException {
    private final String sellerId;
    private final String stripeAccountId;

    public StripeAccountStorageException(String message, String sellerId, String stripeAccountId) {
        super(message);
        this.sellerId = sellerId;
        this.stripeAccountId = stripeAccountId;
    }

    public StripeAccountStorageException(String message, String sellerId, String stripeAccountId, Throwable cause) {
        super(message, cause);
        this.sellerId = sellerId;
        this.stripeAccountId = stripeAccountId;
    }

}