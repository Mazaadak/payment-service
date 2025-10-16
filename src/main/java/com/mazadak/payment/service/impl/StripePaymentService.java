package com.mazadak.payment.service.impl;

import com.mazadak.payment.constant.PaymentConstants;
import com.mazadak.payment.dto.request.CartItem;
import com.mazadak.payment.dto.request.RefundRequest;
import com.mazadak.payment.dto.response.RefundResponse;
import com.mazadak.payment.exception.PaymentProcessingException;
import com.mazadak.payment.exception.RefundFailureException;
import com.mazadak.payment.exception.ResourceNotFoundException;
import com.mazadak.payment.model.SellerStripeAccount;
import com.mazadak.payment.model.StripeChargeTransaction;
import com.mazadak.payment.model.StripeTransferTransaction;
import com.mazadak.payment.repository.SellerStripeAccountRepository;
import com.mazadak.payment.repository.StripeChargeTransactionRepository;
import com.mazadak.payment.repository.StripeTransferTransactionRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.Refund;
import com.stripe.model.Transfer;
import com.stripe.net.RequestOptions;
import com.stripe.param.TransferCreateParams;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class StripePaymentService implements IPaymentService {

    @Value("${stripe.api.secret-key}")
    private String secretKey;

    private final StripeChargeTransactionRepository stripeChargeTransactionRepository;
    private final StripeTransferTransactionRepository stripeTransferTransactionRepository;
    private final SellerStripeAccountRepository sellerStripeAccountRepository;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }

    @Override
    @Transactional
    public StripePaymentResponse process(StripePaymentRequest request) {
        log.info("Received marketplace request for order: {}", request.orderId());


        BigDecimal totalAmount = request.items().stream()
                .map(CartItem::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalAmountInCents = totalAmount.multiply(new BigDecimal("100")).longValue();
        Charge charge;
        StripeChargeTransaction parentTransaction;
        try {
            ChargeCreateParams params = ChargeCreateParams.builder()
                    .setAmount(totalAmountInCents)
                    .setCurrency(request.currency().toLowerCase())
                    .setSource(request.paymentToken())
                    .setDescription("Charge for order " + request.orderId())
                    .build();

            RequestOptions requestOptions = RequestOptions.builder()
                    .setIdempotencyKey(request.idempotencyKey())
                    .build();

            charge = Charge.create(params, requestOptions);

            parentTransaction = buildTransaction(request, totalAmount, charge.getId(), PaymentConstants.CHARGE_SUCCESS, null);
            stripeChargeTransactionRepository.save(parentTransaction);

            log.info("Successfully created charge :{} for order: {}",charge.getId(), request.orderId());
        } catch (StripeException e) {
            log.error("Failed to created charge for order: {}",request.orderId(), e.getMessage());
            parentTransaction = buildTransaction(request, totalAmount, null, PaymentConstants.CHARGE_FAILURE, e.getMessage());
            stripeChargeTransactionRepository.save(parentTransaction);

            throw new PaymentProcessingException("Stripe payment failed: " + e.getMessage(), request.orderId());
//            return new StripePaymentResponse(null, request.orderId(), "FAILURE");
        }

        Map<String, BigDecimal> sellerTotals = request.items().stream()
                .collect(Collectors.groupingBy(
                        CartItem::sellerId,
                        Collectors.mapping(CartItem::amount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        List<String> transferIds = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : sellerTotals.entrySet()) {
            String sellerId = entry.getKey();
            BigDecimal sellerAmount = entry.getValue();
            sellerAmount = sellerAmount.subtract(sellerAmount.multiply(PaymentConstants.PLATFORM_COMMISSION)); ///commission

            log.info("amount {} will be transfer after commission for seller {}", sellerAmount, sellerId);

            long sellerAmountInCents = sellerAmount.multiply(new BigDecimal("100")).longValue();

            try {
                String stripeAccountId = getStripeAccountId(sellerId);

                TransferCreateParams transferParams = TransferCreateParams.builder()
                        .setAmount(sellerAmountInCents)
                        .setCurrency(request.currency().toLowerCase())
                        .setDestination(stripeAccountId)
                        .setSourceTransaction(charge.getId())
                        .build();

                Transfer transfer = Transfer.create(transferParams);
                transferIds.add(transfer.getId());

                StripeTransferTransaction transferTransaction = buildTransferTransaction(parentTransaction, sellerId, sellerAmount, transfer.getId(), "SUCCESS", null);
                stripeTransferTransactionRepository.save(transferTransaction);
                log.info("Successfully transferred {} to seller {} for orderId {}", sellerAmount, sellerId, request.orderId());

            } catch (StripeException | ResourceNotFoundException e) {
                log.error("Failed to transfer funds to seller {} for orderId {}: {}", sellerId, request.orderId(), e.getMessage());
                StripeTransferTransaction transferTransaction = buildTransferTransaction(parentTransaction, sellerId, sellerAmount, null, "FAILURE", e.getMessage());
                stripeTransferTransactionRepository.save(transferTransaction);
            }
        }

        return new StripePaymentResponse(charge.getId(), "PROCESSED", transferIds, "payment processed");
    }


    @Transactional
    public RefundResponse refundPayment(RefundRequest refundRequest) {
        log.info("Processing refund for orderId: {} with idempotency key: {}", refundRequest.orderId(), refundRequest.idempotencyKey());
        StripeChargeTransaction transaction = stripeChargeTransactionRepository.findByOrderId(refundRequest.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "orderId", refundRequest.orderId()));

        if (transaction.getRefunded().equals(Boolean.TRUE))
            throw new RefundFailureException("This transaction has already been refunded");
        if (!transaction.getStatus().equals(PaymentConstants.CHARGE_SUCCESS))
            throw new RefundFailureException("Cannot refund a transaction that was not successful");

        try {
            RefundCreateParams params = RefundCreateParams.builder()
                    .setCharge(transaction.getStripeChargeId())
                    .build();

            RequestOptions requestOptions = RequestOptions.builder().setIdempotencyKey(refundRequest.idempotencyKey()).build();
            Refund refund = Refund.create(params,requestOptions);

            transaction.setStatus("REFUNDED");
            transaction.setRefunded(true);
            stripeChargeTransactionRepository.save(transaction);

            for (StripeTransferTransaction transfer : transaction.getTransfers()) {
                transfer.setStatus("REVERSED");
                stripeTransferTransactionRepository.save(transfer);
            }

            log.info("Successfully refunded charge {} for orderId {}. Refund ID: {}", transaction.getStripeChargeId(), refundRequest.orderId(), refund.getId());
            return new RefundResponse(refund.getId(), refundRequest.orderId(), refund.getStatus(), "Refund processed successfully");
        } catch (StripeException e) {
            log.error("Failed to refund charge for orderId {}: {}", refundRequest.orderId(), e.getMessage());
            throw new RefundFailureException("Stripe refund failed: " + e.getMessage());
        }
    }

    private StripeChargeTransaction buildTransaction(StripePaymentRequest request, BigDecimal totalAmount, String stripeChargeId, String status, String stripeErrorMessage) {
        return StripeChargeTransaction.builder()
                .orderId(request.orderId())
                .stripeChargeId(stripeChargeId)
                .amount(totalAmount)
                .currency(request.currency())
                .status(status)
                .idempotencyKey(request.idempotencyKey())
                .stripeErrorMessage(stripeErrorMessage)
                .refunded(false)
                .build();
    }

    private StripeTransferTransaction buildTransferTransaction(StripeChargeTransaction parent, String sellerId, BigDecimal amount, String stripeTransferId, String status, String stripeErrorMessage) {
        return StripeTransferTransaction.builder()
                .chargeTransaction(parent)
                .stripeTransferId(stripeTransferId)
                .sellerStripeAccountId(getStripeAccountId(sellerId))
                .amount(amount)
                .currency(parent.getCurrency())
                .status(status)
                .stripeErrorMessage(stripeErrorMessage)
                .build();
    }

    public String getStripeAccountId(String sellerId) {
        SellerStripeAccount sellerStripeAccount = sellerStripeAccountRepository.findBySellerId(sellerId);
        if (sellerStripeAccount == null)
            throw new ResourceNotFoundException("SellerStripeAccount", "sellerId", sellerId);

        return sellerStripeAccount.getStripeAccountId();
    }

    public Page<StripeChargeTransaction> getTransactionsPage(Pageable pageable) {
        log.info("Fetching transactions for page request: {}", pageable);
        return stripeChargeTransactionRepository.findAll(pageable);
    }

    public StripeChargeTransaction getTransactionByOrderId(String orderId) {
        log.info("Fetching transaction for orderId: {}", orderId);
        Optional<StripeChargeTransaction> trans = stripeChargeTransactionRepository.findByOrderId(orderId);
        if (trans.isEmpty())
            throw new ResourceNotFoundException("StripeTransaction", "orderId", orderId);

        return trans.get();
    }

}
