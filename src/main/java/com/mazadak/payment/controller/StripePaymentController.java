package com.mazadak.payment.controller;

import com.mazadak.payment.dto.request.CreatePaymentIntentRequest;
import com.mazadak.payment.dto.request.RefundRequest;
import com.mazadak.payment.dto.response.CreatePaymentIntentResponse;
import com.mazadak.payment.dto.response.PaymentIntentResponse;
import com.mazadak.payment.dto.response.RefundResponse;
import com.mazadak.payment.service.impl.StripePaymentService;
import com.stripe.model.PaymentIntent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@Tag(name = "Payment Controller", description = "APIs for processing marketplace payments")
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Validated
public class StripePaymentController {
    private final StripePaymentService paymentService;

    @Operation(summary = "Create a Payment Intent (Authorize)",
            description = "Initiates a payment by creating a PaymentIntent...The response contains a client secret that the frontend uses to confirm the payment")
    @PostMapping("/create-payment-intent")
    public ResponseEntity<CreatePaymentIntentResponse> createPaymentIntent(@Valid @RequestBody CreatePaymentIntentRequest request) {
        CreatePaymentIntentResponse response = paymentService.createPaymentIntent(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Stripe Webhook Handler",
            description = "Handles asynchronous events from Stripe, such as payment success or failure. This endpoint is called by Stripe, not by the client.")
    @PostMapping("/stripe-webhook")
    public ResponseEntity<Void> handleStripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        paymentService.handleStripeWebhook(payload, sigHeader);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Capture a Payment Intent",
            description = "Captures the funds for a previously authorized payment. This is the action that actually charges the customer's card.")
    @PostMapping("/{orderId}/capture")
    public ResponseEntity<PaymentIntentResponse> capturePayment(@PathVariable UUID orderId) {
        PaymentIntent capturedPaymentIntent = paymentService.capturePayment(orderId);
        return ResponseEntity.ok(PaymentIntentResponse.from(capturedPaymentIntent));
    }

    @Operation(summary = "Cancel a Payment Intent",
            description = "Cancels a previously authorized payment, releasing the hold on the customer's card. This can only be done before the payment is captured.")
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<PaymentIntentResponse> cancelPayment(@PathVariable UUID orderId) {
        PaymentIntent canceledPaymentIntent = paymentService.cancelPayment(orderId);
        return ResponseEntity.ok(PaymentIntentResponse.from(canceledPaymentIntent));
    }

    @Operation(summary = "Refund a payment",
            description = "Processes a full refund for a previously successful transaction.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Refund processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid refund request"),
            @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    @PostMapping("/refund")
    public ResponseEntity<RefundResponse> refundPayment(@Valid @RequestBody RefundRequest request) {
        RefundResponse response = paymentService.refundPayment(request);
        return ResponseEntity.ok(response);
    }

}
