package com.mazadak.payment.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Response for a multi-seller payment operation, detailing the overall charge and individual transfers.")
public record StripePaymentResponse(
        @Schema(description = "The unique ID of the main charge created for the entire cart.")
        String chargeId,

        @Schema(description = "The overall status of the payment operation.")
        String status,

        @Schema(description = "A list of IDs for the individual transfers made to each seller.")
        List<String> transferIds,

        @Schema(description = "A message summarizing the outcome of the operation.")
        String message
) {}
