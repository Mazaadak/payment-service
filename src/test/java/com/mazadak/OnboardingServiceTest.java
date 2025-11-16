package com.mazadak;

import com.mazadak.payment.repository.SellerStripeAccountRepository;
import com.mazadak.payment.service.impl.OnboardingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @Mock
    private SellerStripeAccountRepository sellerStripeAccountRepository;

    @InjectMocks
    private OnboardingService onboardingService;

    private static final String TEST_REDIRECT_URL = "https://yourapp.com/success";

    @BeforeEach
    void setUp() {
        // Re-initialize the service with constructor arguments for each test
        onboardingService = new OnboardingService(sellerStripeAccountRepository);
        onboardingService.setStripeClientId("test_client_id");
        onboardingService.setStripeSecretKey("test_secret_key");
        onboardingService.init(); // Manually trigger PostConstruct method
    }

    @Test
    void generateOnboardingUrl_withValidSellerId_returnsCorrectUrl() {
        UUID sellerId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        String url = onboardingService.generateOnboardingUrl(sellerId, TEST_REDIRECT_URL);

        assertNotNull(url);
        assertTrue(url.startsWith("https://connect.stripe.com/oauth/authorize"));
        assertTrue(url.contains("client_id=test_client_id"));
        assertTrue(url.contains("scope=read_write"));
        assertTrue(url.contains("response_type=code"));
        assertTrue(url.contains("state="));
        // The state parameter now contains Base64-encoded JSON with sellerId and redirectUrl
    }

    @Test
    void generateOnboardingUrl_withNullSellerId_throwsIllegalArgumentException() {
        UUID sellerId = null;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            onboardingService.generateOnboardingUrl(sellerId, TEST_REDIRECT_URL);
        });

        assertEquals("sellerId is required", exception.getMessage());
    }

    @Test
    void generateOnboardingUrl_withNullRedirectUrl_throwsIllegalArgumentException() {
        UUID sellerId = UUID.randomUUID();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            onboardingService.generateOnboardingUrl(sellerId, null);
        });

        assertEquals("redirectUrl is required", exception.getMessage());
    }

    @Test
    void generateOnboardingUrl_withBlankRedirectUrl_throwsIllegalArgumentException() {
        UUID sellerId = UUID.randomUUID();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            onboardingService.generateOnboardingUrl(sellerId, "   ");
        });

        assertEquals("redirectUrl is required", exception.getMessage());
    }
}