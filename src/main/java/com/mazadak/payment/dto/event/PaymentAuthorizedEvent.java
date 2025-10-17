package com.mazadak.payment.dto.event;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;
@Schema(description = "Event triggered when authorizes the payment")
public record PaymentAuthorizedEvent(
        @Schema(description = "Stripe Payment Intent ID")
        String paymentIntentId,
        @Schema(description = "UUID string form id")
        String orderId,
        @Schema(description = "Checkout from fixed or auction", example = "AUCTION")
        String checkoutType,
        @Schema(description = "amount in cents")
        BigDecimal amount
) {}
