package com.mazadak;

import com.mazadak.payment.service.impl.OnboardingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class PaymentServiceApplicationTests {

    @InjectMocks
    private OnboardingService onboardingService;

    @BeforeEach
    void setUp() {
        // Initialize the service and inject mock values for properties
        onboardingService = new OnboardingService();
        ReflectionTestUtils.setField(onboardingService, "stripeClientId", "test_client_id");
        ReflectionTestUtils.setField(onboardingService, "stripeSecretKey", "test_secret_key");
        onboardingService.init(); // Manually trigger PostConstruct method
    }

    @Test
    void generateOnboardingUrl_withValidSellerId_returnsCorrectUrl() {
        String sellerId = "seller123";

        String url = onboardingService.generateOnboardingUrl(sellerId);

        assertNotNull(url);
        assertTrue(url.startsWith("https://connect.stripe.com/oauth/authorize"));
        assertTrue(url.contains("client_id=test_client_id"));
        assertTrue(url.contains("scope=read_write"));
        assertTrue(url.contains("response_type=code"));
        assertTrue(url.contains("state=seller123"));
    }

    @Test
    void generateOnboardingUrl_withNullSellerId_throwsIllegalArgumentException() {
        String sellerId = null;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            onboardingService.generateOnboardingUrl(sellerId);
        });

        assertEquals("sellerId is required", exception.getMessage());
    }

    @Test
    void generateOnboardingUrl_withBlankSellerId_throwsIllegalArgumentException() {
        String sellerId = " ";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            onboardingService.generateOnboardingUrl(sellerId);
        });

        assertEquals("sellerId is required", exception.getMessage());
    }
}
