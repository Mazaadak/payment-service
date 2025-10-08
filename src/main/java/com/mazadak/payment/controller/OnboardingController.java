package com.mazadak.payment.controller;

import com.mazadak.payment.constant.OnboardingConstants;
import com.mazadak.payment.service.impl.OnboardingService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.net.OAuth;
import com.mazadak.payment.exception.SellerServiceException;
import com.mazadak.payment.exception.StripeOAuthException;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import io.swagger.v3.oas.annotations.Parameter;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "Stripe Onboarding", description = "APIs for handling Stripe Connect account onboarding")
@RestController
@RequestMapping("/api/onboarding")
@CrossOrigin(origins = "*")
@Slf4j
@AllArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

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
                    description = "Payload containing the seller ID",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(type = "object", example = "{\"sellerId\": \"1254\"}"),
                            examples = @ExampleObject(name = "Seller ID Example", value = "{\"sellerId\": \"1254\"}")
                    )) @org.springframework.web.bind.annotation.RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(Map.of("onboardingUrl", onboardingService.generateOnboardingUrl(payload.get("sellerId"))));
    }
}
