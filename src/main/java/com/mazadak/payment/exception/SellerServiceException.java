package com.mazadak.payment.exception;

import lombok.Getter;

@Getter
public class SellerServiceException extends RuntimeException {
    private final String sellerId;
    private final int statusCode;

    public SellerServiceException(String message, String sellerId, int statusCode) {
        super(message);
        this.sellerId = sellerId;
        this.statusCode = statusCode;
    }

    public SellerServiceException(String message, String sellerId, int statusCode, Throwable cause) {
        super(message, cause);
        this.sellerId = sellerId;
        this.statusCode = statusCode;
    }

}