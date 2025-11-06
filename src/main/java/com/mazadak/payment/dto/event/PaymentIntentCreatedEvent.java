package com.mazadak.payment.dto.event;

public record PaymentIntentCreatedEvent(String paymentIntentId, String clientSecret, String orderId) {
}
