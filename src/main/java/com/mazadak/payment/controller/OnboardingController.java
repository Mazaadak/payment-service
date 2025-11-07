package com.mazadak.payment.controller;

import com.mazadak.payment.constant.OnboardingConstants;
import com.mazadak.payment.service.impl.OnboardingService;
import com.mazadak.payment.service.impl.StripePaymentService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.net.OAuth;
import com.mazadak.payment.exception.StripeOAuthException;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static com.mazadak.payment.utils.OnboardingUtils.extractRedirectUrlFromState;
import static com.mazadak.payment.utils.OnboardingUtils.extractSellerIdFromState;

@Tag(name = "Stripe Onboarding", description = "APIs for handling Stripe Connect account onboarding")
@RestController
@RequestMapping("/api/onboarding")
@Slf4j
@AllArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;
    private final StripePaymentService paymentService;

    @Operation(summary = "Generate Stripe OAuth URL",
            description = "Generates a Stripe Connect OAuth URL for seller onboarding")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OAuth URL generated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(value = "{\"onboardingUrl\": \"https://connect.stripe.com/oauth/v2/authorize?response_type=code&client_id=acct_YOUR_CLIENT_ID&scope=read_write&redirect_uri=YOUR_REDIRECT_URL\"}"))),
            @ApiResponse(responseCode = "400", description = "Invalid seller ID provided",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(value = "{\"error\": \"Seller ID is required\"}"))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(value = "{\"error\": \"An unexpected error occurred\"}")))
    })
    @PostMapping("/oauth/url")
    public ResponseEntity<Map<String, String>> getOAuthUrl(
            @RequestBody(
                    description = "Payload containing the seller ID and redirect URL",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(type = "object", example = "{\"sellerId\": \"1254\", \"redirectUrl\": \"https://yourapp.com/success\"}"),
                            examples = @ExampleObject(name = "Request Example", value = "{\"sellerId\": \"UUID\", \"redirectUrl\": \"https://yourapp.com/success\"}")
                    )) @org.springframework.web.bind.annotation.RequestBody Map<String, String> payload) {

        UUID sellerId = UUID.fromString(payload.get("sellerId"));
        String redirectUrl = payload.get("redirectUrl");

        return ResponseEntity.ok(Map.of("onboardingUrl", onboardingService.generateOnboardingUrl(sellerId, redirectUrl).toString()));
    }

    @Operation(summary = "Handle Stripe OAuth Callback",
            description = "Handles the callback from Stripe after a seller completes the OAuth process " +
                    "Exchanges the authorization code for an access token and connects the Stripe account to the seller")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stripe account id created successfully",
                    content = @Content(mediaType = "text/html",
                            examples = @ExampleObject(value = "<h1>Stripe Account Connected</h1><p>accountId: (acct_12345)</p>"))),
            @ApiResponse(responseCode = "400", description = "Invalid authorization code or state",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(value = "{\"error\": \"Invalid authorization code or seller ID\"}"))),
            @ApiResponse(responseCode = "500", description = "Internal server error during OAuth token exchange",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Map.class),
                            examples = @ExampleObject(value = "{\"error\": \"Failed to connect Stripe account\"}")))
    })
    @GetMapping("/oauth/callback")
    public ResponseEntity<Void> getOAuthCallback(
            @RequestParam("code") String authorizationCode,
            @RequestParam("state") String encodedState) {

        String stateJson = new String(Base64.getUrlDecoder().decode(encodedState), StandardCharsets.UTF_8);

        UUID sellerId = extractSellerIdFromState(stateJson);
        String redirectUrl = extractRedirectUrlFromState(stateJson);

        String connectedAccountId = onboardingService.handleOAuthCallback(authorizationCode, sellerId);

        URI redirectUri = UriComponentsBuilder.fromUriString(redirectUrl)
                .queryParam("success", "true")
                .queryParam("accountId", connectedAccountId)
                .build()
                .toUri();

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(redirectUri)
                .build();
    }

    @Operation(summary = "Get Stripe Account Id for certain Seller")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Get Stripe account id"),
            @ApiResponse(responseCode = "404", description = "Seller or Stripe account not found")
    })
    @GetMapping("/get-account/{sellerId}")
    public ResponseEntity<String> getConnectedAccountId(@PathVariable UUID sellerId) {
        return ResponseEntity.ok(paymentService.getStripeAccountId(sellerId));
    }
}
