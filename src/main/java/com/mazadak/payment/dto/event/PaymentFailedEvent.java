package com.mazadak.payment.dto.event;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Event indicating a failed payment for an order")
public record PaymentFailedEvent(
        @Schema(description = "Stripe Payment Intent ID")
        String paymentIntentId,
        @Schema(description = "Order Id", example = "order-1234 or UUID")
        String orderId,
        @Schema(description = "Reason of failure")
        String failureReason) {
}
