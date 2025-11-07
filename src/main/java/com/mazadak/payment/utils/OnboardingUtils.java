package com.mazadak.payment.utils;

import java.util.UUID;

public class OnboardingUtils {
    public static UUID extractSellerIdFromState(String stateJson) {
        String sellerId = stateJson.split("\"sellerId\":\"")[1].split("\"")[0];
        return UUID.fromString(sellerId);
    }

    public static String extractRedirectUrlFromState(String stateJson) {
        String redirectUrl = stateJson.split("\"redirectUrl\":\"")[1].split("\"")[0];
        return redirectUrl;
    }
}
