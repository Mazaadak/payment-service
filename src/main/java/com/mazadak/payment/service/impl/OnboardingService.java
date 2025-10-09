package com.mazadak.payment.service.impl;

import com.mazadak.payment.constant.OnboardingConstants;
import com.mazadak.payment.exception.StripeOAuthException;
import com.mazadak.payment.model.SellerStripeAccount;
import com.mazadak.payment.repository.SellerStripeAccountRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.net.OAuth;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class OnboardingService {

    @Value("${stripe.api.client-id}")
    private String stripeClientId;

    @Value("${stripe.api.secret-key}")
    private String stripeSecretKey;

    private final SellerStripeAccountRepository sellerStripeAccountRepository;


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

    public String handleOAuthCallback(String authorizationCode, String sellerId) {

        log.info("Processing OAuth callback for seller: {}", sellerId);

        /// Apply Server to Server communication -> to get the stripe account id
        String connectedAccountId = exchangeCodeForAccountId(authorizationCode, sellerId);

        /// Store the stripe account id in the seller service
        storeStripeAccount(sellerId, connectedAccountId);

        log.info("Successfully stored Stripe Account: : {}", connectedAccountId);

        return connectedAccountId;
    }

    private String exchangeCodeForAccountId(String authorizationCode, String sellerId) {
        Map<String, Object> params = new HashMap<>();
        params.put("grant_type", "authorization_code");
        params.put("code", authorizationCode);

        try {
            /// Communicate with stripe oauth api to extract the actual account id
            var response = OAuth.token(params, null);
            String connectedAccountId = response.getStripeUserId();

            if (connectedAccountId == null)
                throw new StripeOAuthException("Could not retrieve Stripe User ID from OAuth response", sellerId);

            return connectedAccountId;
        } catch (StripeException e) {
            throw new StripeOAuthException("Stripe OAuth failed: " + e.getMessage(), sellerId, e);
        }
    }

    void storeStripeAccount(String sellerId, String connectedAccountId){
        SellerStripeAccount sellerStripeAccount = new SellerStripeAccount();
        sellerStripeAccount.setSellerId(sellerId);
        sellerStripeAccount.setStripeAccountId(connectedAccountId);
        sellerStripeAccountRepository.save(sellerStripeAccount);
    }

    /// TODO: Implement the deauthorize account id -> when the user is deleted
}
