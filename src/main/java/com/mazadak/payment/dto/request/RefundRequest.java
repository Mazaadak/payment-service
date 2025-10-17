package com.mazadak.payment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "The request body for initiating a refund. It requires the orderId of the original transaction and a new idempotency key for the refund operation")
public record RefundRequest(

        @Schema(description = "The unique identifier of the order for which the refund is being requested",
                example = "order-abc-123")
        @NotNull
        UUID orderId,

        @Schema(description = "**New**, unique key to make the refund operation idempotent. Do NOT reuse the key from the original payment",
                example = "refund-a1b2c3d4-e5f6-7890")
        @NotNull(message = "A unique idempotency key is required for the refund operation")
        UUID idempotencyKey
) {}
