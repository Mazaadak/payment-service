package com.mazadak.payment.controller;

import com.mazadak.payment.dto.request.RefundRequest;
import com.mazadak.payment.dto.request.StripePaymentRequest;
import com.mazadak.payment.dto.response.PageResponse;
import com.mazadak.payment.dto.response.RefundResponse;
import com.mazadak.payment.dto.response.StripePaymentResponse;
import com.mazadak.payment.model.StripeChargeTransaction;
import com.mazadak.payment.service.impl.StripePaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    @Operation(summary = "Get transactions page",
            description = "Retrieves a paginated list of all Stripe transactions.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of transactions"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/transactions")
    public ResponseEntity<PageResponse<StripeChargeTransaction>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        Page<StripeChargeTransaction> transactions = paymentService.getTransactionsPage(PageRequest.of(page, size));
        return ResponseEntity.ok(new PageResponse<>(transactions));
    }

    @Operation(summary = "Get transaction by order ID",
            description = "Retrieves a single Stripe transaction by its unique order ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction found and retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Transaction not found for the given order ID"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/transactions/{orderId}")
    public ResponseEntity<StripeChargeTransaction> getTransactionByOrderId(@PathVariable String orderId) {
        StripeChargeTransaction transaction = paymentService.getTransactionByOrderId(orderId);
        return ResponseEntity.ok(transaction);
    }


    @Operation(summary = "Refund a payment",
            description = "Processes a full refund for a previously successful transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Refund processed successfully",
                    content = @Content(schema = @Schema(implementation = RefundResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid refund request (e.g., transaction not successful, already refunded)"),
            @ApiResponse(responseCode = "404", description = "Transaction not found for the given order ID"),
            @ApiResponse(responseCode = "500", description = "Internal server error during refund processing")
    })
    @PostMapping("/refund")
    public ResponseEntity<RefundResponse> refundPayment(@Valid @RequestBody RefundRequest request) {
        RefundResponse response = paymentService.refundPayment(request);
        return ResponseEntity.ok(response);
    }
}
