package com.mazadak.payment.constant;

public class PaymentStates {
    public static final String PENDING = "PENDING";
    public static final String REQUIRES_CAPTURE = "REQUIRES_CAPTURE";
    public static final String FAILED = "FAILED";
    public static final String SUCCEEDED = "SUCCEEDED";
    public static final String REFUNDED = "REFUNDED";
    public static final String CANCELED = "CANCELED";
    public static final String REVERSED = "REVERSED";


    private PaymentStates(){}
}
