package com.mazadak.payment.dto.request;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Request object for processing Stripe payments")
public record StripePaymentRequest(

        @Schema(description = "Order ID", example = "cart-xyz-987")
        @NotBlank
        String orderId,

        @Schema(description = "The payment token (e.g., 'tok_...') (obtained from Stripe.js)")
        @NotBlank
        String paymentToken,

        @Schema(description = "currency code for the transaction", example = "usd")
        @NotBlank
        String currency,

        @Schema(description = "Unique key to make idempotent", example = "payment-{order_id}" )
        @NotBlank
        String idempotencyKey,

        @Schema(description = "A list of items in the cart, each with a seller and an amount")
        @NotEmpty
        @Valid
        List<CartItem> items
) {
}
