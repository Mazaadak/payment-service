package com.mazadak.payment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response object containing payment processing results")
public record StripePaymentResponse(
    @Schema(description = "Stripe payment intent ID")
    String transactionId,

    @Schema(description = "Original order ID reference")
    String orderId,

    @Schema(description = "Payment status", example = "succeeded")
    String status
) {}
