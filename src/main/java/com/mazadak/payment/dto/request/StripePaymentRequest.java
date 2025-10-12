package com.mazadak.payment.dto.request;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Request object for processing Stripe payments")
public record StripePaymentRequest(
        @Schema(description = "Unique identifier for the order", example = "ord_123456")
        @NotBlank(message = "Order ID is required")
        String orderId,

        @Schema(description = "Payment amount", example = "99.99")
        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        BigDecimal amount,

        @Schema(description = "Currency code in ISO format", example = "USD")
        @NotBlank(message = "Currency is required")
        String currency,

        @Schema(description = "Stripe payment token/method ID", example = "pm_123456789")
        @NotBlank(message = "Payment token is required")
        String paymentToken,

        @Schema(description = "Seller's Stripe Connect account ID", example = "acct_123456")
        @NotBlank(message = "Seller Stripe account ID is required")
        String sellerStripeAccountId,

        @Schema(description = "Unique key to prevent duplicate processing", example = "payment-9bdb72f0-835f-4cb8-87bd-b41ab68d54b1-1760246986034")
        @NotBlank(message = "Idempotency key is required")
        String idempotencyKey
) {
}
