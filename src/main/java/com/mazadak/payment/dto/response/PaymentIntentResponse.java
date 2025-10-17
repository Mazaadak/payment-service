package com.mazadak.payment.dto.response;

import com.stripe.model.PaymentIntent;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A simplified response object containing key details from a Stripe PaymentIntent")
public record PaymentIntentResponse(
        @Schema(description = "The ID of the PaymentIntent.")
        String id,

        @Schema(description = "The status of the PaymentIntent.")
        String status,

        @Schema(description = "The amount to be captured.")
        Long amount,

        @Schema(description = "The currency of the transaction.")
        String currency
) {
    public static PaymentIntentResponse from(PaymentIntent paymentIntent) {
        return new PaymentIntentResponse(
                paymentIntent.getId(),
                paymentIntent.getStatus(),
                paymentIntent.getAmount(),
                paymentIntent.getCurrency()
        );
    }
}
