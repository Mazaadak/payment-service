package com.mazadak.payment.service;

import com.mazadak.payment.dto.request.StripePaymentRequest;
import com.mazadak.payment.dto.response.StripePaymentResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payment processing service interface")
public interface IPaymentService {
    @Schema(description = "Process a payment through the payment provider")
    StripePaymentResponse process(StripePaymentRequest request);
}
