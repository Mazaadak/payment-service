package com.mazadak.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Represents a required item info for payments")
public record CartItem(
        @Schema(description = "ID of the seller for this item", example = "seller-123")
        @NotBlank
        String sellerId,

        @Schema(description = "Price of the item", example = "50.00")
        @NotNull
        @Positive
        BigDecimal amount
) {}
