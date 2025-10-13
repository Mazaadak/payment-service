package com.mazadak.payment.service.impl;

import com.mazadak.payment.dto.request.RefundRequest;
import com.mazadak.payment.dto.response.RefundResponse;
import com.mazadak.payment.exception.PaymentProcessingException;
import com.mazadak.payment.exception.RefundFailureException;
import com.mazadak.payment.exception.ResourceNotFoundException;
import com.mazadak.payment.model.SellerStripeAccount;
import com.mazadak.payment.model.StripeTransaction;
import com.mazadak.payment.repository.SellerStripeAccountRepository;
import com.mazadak.payment.repository.StripeTransactionRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.param.ChargeCreateParams;
import com.stripe.param.RefundCreateParams;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import com.mazadak.payment.dto.request.StripePaymentRequest;
import com.mazadak.payment.dto.response.StripePaymentResponse;
import com.mazadak.payment.service.IPaymentService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class StripePaymentService implements IPaymentService {

    @Value("${stripe.api.secret-key}")
    private String secretKey;

    private final StripeTransactionRepository stripeTransactionRepository;
    private final SellerStripeAccountRepository sellerStripeAccountRepository;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }

    @Override
    @Transactional
    public StripePaymentResponse process(StripePaymentRequest request) {
        log.info("Received marketplace request for order: {}", request.orderId());

        /// 5% commission
        BigDecimal totalAmount = request.amount();
        BigDecimal commission = totalAmount.multiply(new BigDecimal("0.05"));

        long totalAmountInCents = totalAmount.multiply(new BigDecimal("100")).longValue();
        long commissionInCents = commission.multiply(new BigDecimal("100")).longValue();

        try {
            ChargeCreateParams params = ChargeCreateParams.builder()
                    .setAmount(totalAmountInCents)
                    .setCurrency(request.currency().toLowerCase())
                    .setSource(request.paymentToken())
                    .setDescription("Charge for order " + request.orderId())
                    .setTransferData(
                            ChargeCreateParams.TransferData.builder()
                                    .setDestination(request.sellerStripeAccountId())
                                    .build()
                    )
                    .setApplicationFeeAmount(commissionInCents)
                    .setOnBehalfOf(request.sellerStripeAccountId())
                    .build();

            RequestOptions requestOptions = RequestOptions.builder()
                    .setIdempotencyKey(request.idempotencyKey())
                    .build();

            Charge charge = Charge.create(params, requestOptions);

            stripeTransactionRepository.save(buildTransaction(request, commission, charge.getId(), "SUCCESS", null));

            log.info("Successfully created charge for order {}: {}", request.orderId(), charge.getId());
            return new StripePaymentResponse(charge.getId(), request.orderId(), "SUCCESS");
        } catch (StripeException e) {
            log.error("Failed to create charge for order {}: {}", request.orderId(), e.getMessage());

            stripeTransactionRepository.save(buildTransaction(request, commission, null, "FAILURE", e.getMessage()));

//            throw new PaymentProcessingException("Stripe payment failed: " + e.getMessage(), request.orderId());
            return new StripePaymentResponse(null, request.orderId(), "FAILURE");
        }
    }


    @Transactional
    public RefundResponse refundPayment(RefundRequest refundRequest) {
        log.info("Processing refund for orderId: {} with idempotency key: {}", refundRequest.orderId(), refundRequest.idempotencyKey());
        StripeTransaction transaction = stripeTransactionRepository.findByOrderId(refundRequest.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "orderId", refundRequest.orderId()));

        if (!transaction.getStatus().equals("SUCCESS"))
            throw new RefundFailureException("Cannot refund a transaction that was not successful");
        if (transaction.getRefunded().equals(Boolean.TRUE))
            throw new RefundFailureException("This transaction has already been refunded");

        try {
            RefundCreateParams params = RefundCreateParams.builder()
                    .setCharge(transaction.getStripeChargeId())
                    .setReverseTransfer(true) /// Reverses the transfer made to the seller
                    .setRefundApplicationFee(true) /// Refunds the application fee you took
                    .build();
            RequestOptions requestOptions = RequestOptions.builder().setIdempotencyKey(refundRequest.idempotencyKey()).build();
            Refund refund = Refund.create(params,requestOptions);

            /// Update transaction to mark it as refunded
            transaction.setStatus("REFUNDED");
            transaction.setRefunded(true);
            stripeTransactionRepository.save(transaction);

            log.info("Successfully refunded charge {} for orderId {}. Refund ID: {}", transaction.getStripeChargeId(), refundRequest.orderId(), refund.getId());
            return new RefundResponse(refund.getId(), refundRequest.orderId(), refund.getStatus(), "Refund processed successfully.");
        } catch (StripeException e) {
            log.error("Failed to refund charge for orderId {}: {}", refundRequest.orderId(), e.getMessage());
            throw new RefundFailureException("Stripe refund failed: " + e.getMessage());
        }
    }

    private StripeTransaction buildTransaction(StripePaymentRequest request, BigDecimal commission, String stripeChargeId, String status, String stripeErrorMessage) {
        return StripeTransaction.builder()
                .orderId(request.orderId())
                .stripeChargeId(stripeChargeId)
                .sellerStripeAccountId(request.sellerStripeAccountId())
                .amount(request.amount())
                .commission(commission)
                .currency(request.currency())
                .status(status)
                .idempotencyKey(request.idempotencyKey())
                .stripeErrorMessage(stripeErrorMessage)
                .refunded(false)
                .build();
    }


    public String getStripeAccountId(String sellerId) {
        SellerStripeAccount sellerStripeAccount = sellerStripeAccountRepository.findBySellerId(sellerId);
        if (sellerStripeAccount == null)
            throw new ResourceNotFoundException("SellerStripeAccount", "sellerId", sellerId);

        return sellerStripeAccount.getStripeAccountId();
    }

    public Page<StripeTransaction> getTransactionsPage(Pageable pageable) {
        log.info("Fetching transactions for page request: {}", pageable);
        return stripeTransactionRepository.findAll(pageable);
    }

    public StripeTransaction getTransactionByOrderId(String orderId) {
        log.info("Fetching transaction for orderId: {}", orderId);
        Optional<StripeTransaction> trans = stripeTransactionRepository.findByOrderId(orderId);
        if (trans.isEmpty())
            throw new ResourceNotFoundException("StripeTransaction", "orderId", orderId);

        return trans.get();
    }

}
