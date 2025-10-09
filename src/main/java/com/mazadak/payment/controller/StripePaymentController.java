package com.mazadak.payment.controller;

import com.mazadak.payment.dto.request.StripePaymentRequest;
import com.mazadak.payment.dto.response.StripePaymentResponse;
import com.mazadak.payment.service.impl.StripePaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Payment Controller", description = "APIs for processing marketplace payments")
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Validated
public class StripePaymentController {
    private final StripePaymentService paymentService;

    @Operation(summary = "Process a marketplace payment",
            description = "Processes a payment through Stripe's marketplace platform")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment processed successfully",
                    content = @Content(schema = @Schema(implementation = StripePaymentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid payment request"),
            @ApiResponse(responseCode = "500", description = "Internal server error during payment processing")
    })
    @PostMapping("/process-marketplace")
    public ResponseEntity<StripePaymentResponse> processMarketplacePayment(@Valid @RequestBody StripePaymentRequest request) {
        StripePaymentResponse response = paymentService.process(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Stripe Account ID for a Seller",
            description = "Retrieves the persisted Stripe Account ID for a given seller ID. This is used to identify the destination account for payments.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stripe Account ID retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Seller ID not found or no Stripe account is associated with it"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })


    @GetMapping("/get-stripe-account-id")
    public ResponseEntity<Map<String,Object>> getStripeAccountId(@RequestParam String sellerId) {
        String stripeAccountId = paymentService.getStripeAccountId(sellerId);
        return ResponseEntity.ok(Map.of("stripeAccountId", stripeAccountId));
    }
}