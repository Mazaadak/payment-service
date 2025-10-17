package com.mazadak.payment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response containing the client secret for a PaymentIntent")
public record CreatePaymentIntentResponse(

        @Schema(description = "The client secret from the created PaymentIntent. This is used by the frontend to confirm the payment")
        String clientSecret
) {}
