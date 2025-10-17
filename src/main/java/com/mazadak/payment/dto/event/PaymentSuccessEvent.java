package com.mazadak.payment.dto.event;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Event indicating a successful payment for an order")
public record PaymentSuccessEvent(
        @Schema(description = "Order Id", example = "order-1234 or UUID")
        String orderId,

        @Schema(description = "Checkout from fixed or auction", example = "AUCTION")
        String checkoutType
) { }
