package com.mazadak.payment.dto.event;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Event indicating a failed payment for an order")
public record PaymentFailedEvent(
        @Schema(description = "Order Id", example = "order-1234 or UUID")
        String orderId,
        @Schema(description = "Reason of failure")
        String failureReason) {
}
