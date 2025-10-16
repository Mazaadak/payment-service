package com.mazadak.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "Request to create a new PaymentIntent")
public record CreatePaymentIntentRequest(

        @Schema(description = "Order ID", example = "cart-xyz-987")
        @NotBlank
        String orderId,

        @Schema(description = "Currency code", example = "usd")
        @NotBlank
        String currency,

        @Schema(description = "A list of items in the cart that contain seller id and item amount")
        @NotEmpty
        @Valid
        List<CartItem> items
) {}
