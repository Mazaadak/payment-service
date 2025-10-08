package com.mazadak.payment.service.impl;

import com.mazadak.payment.constant.OnboardingConstants;
import com.stripe.Stripe;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
public class OnboardingService {

    @Value("${stripe.api.client-id}")
    private String stripeClientId;

    @Value("${stripe.api.secret-key}")
    private String stripeSecretKey;


    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }



    public String generateOnboardingUrl(String sellerId) {
        if (sellerId == null || sellerId.isBlank())
            throw new IllegalArgumentException("sellerId is required");


        String url = UriComponentsBuilder.fromUriString(OnboardingConstants.STRIPE_AUTHORIZE_URI)
                .queryParam("response_type", "code")
                .queryParam("client_id", stripeClientId)
                .queryParam("scope", "read_write")
                .queryParam("redirect_uri", OnboardingConstants.ONBOARDING_REDIRECT_URI)
                .queryParam("state", sellerId)
                .toUriString();

        return url;
    }

}
