package com.mazadak.payment.service.impl;

import com.mazadak.payment.constant.PaymentStates;
import com.mazadak.payment.dto.event.PaymentAuthorizedEvent;
import com.mazadak.payment.dto.event.PaymentFailedEvent;
import com.mazadak.payment.dto.event.PaymentIntentCreatedEvent;
import com.mazadak.payment.dto.event.PaymentSuccessEvent;
import com.mazadak.payment.dto.request.CartItem;
import com.mazadak.payment.dto.request.CreatePaymentIntentRequest;
import com.mazadak.payment.dto.request.RefundRequest;
import com.mazadak.payment.dto.response.CreatePaymentIntentResponse;
import com.mazadak.payment.dto.response.RefundResponse;
import com.mazadak.payment.exception.PaymentProcessingException;
import com.mazadak.payment.exception.ResourceNotFoundException;
import com.mazadak.payment.model.OrderItem;
import com.mazadak.payment.model.SellerStripeAccount;
import com.mazadak.payment.model.StripeChargeTransaction;
import com.mazadak.payment.model.StripeTransferTransaction;
import com.mazadak.payment.repository.SellerStripeAccountRepository;
import com.mazadak.payment.repository.StripeChargeTransactionRepository;
import com.mazadak.payment.repository.StripeTransferTransactionRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class StripePaymentService {

    @Value("${stripe.api.secret-key}")
    private String secretKey;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    private final StripeChargeTransactionRepository stripeChargeTransactionRepository;
    private final StripeTransferTransactionRepository stripeTransferTransactionRepository;
    private final SellerStripeAccountRepository sellerStripeAccountRepository;
    private final StreamBridge streamBridge;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }

    @Transactional
    public CreatePaymentIntentResponse createPaymentIntent(CreatePaymentIntentRequest request) {
        log.info("Creating PaymentIntent for orderId: {}", request.orderId());

        BigDecimal totalAmount = calculateTotalAmount(request.items());
        long totalAmountInCents = totalAmount.multiply(new BigDecimal("100")).longValue();

        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(totalAmountInCents)
                    .setCurrency(request.currency().toLowerCase())
                    .setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL)
                    .putMetadata("orderId", request.orderId().toString())
                    .putMetadata("checkoutType", request.type())
                    .build();
            UUID idempotencyKey = UUID.randomUUID();
            RequestOptions requestOptions = RequestOptions.builder().setIdempotencyKey(idempotencyKey.toString()).build();
            PaymentIntent paymentIntent = PaymentIntent.create(params, requestOptions);

            StripeChargeTransaction chargeTransaction = buildChargeTransaction(request, totalAmount, paymentIntent, idempotencyKey);
            stripeChargeTransactionRepository.save(chargeTransaction);
            log.info("Successfully created PaymentIntent {} for orderId {}", paymentIntent.getId(), request.orderId());
            return new CreatePaymentIntentResponse(paymentIntent.getClientSecret());

        } catch (StripeException e) {
            log.error("Failed to create PaymentIntent for orderId {}: {}", request.orderId(), e.getMessage());
            throw new PaymentProcessingException("Stripe PaymentIntent creation failed: " + e.getMessage());
        }
    }

    private BigDecimal calculateTotalAmount(List<CartItem> items) {
        return items.stream()
                .map(CartItem::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public PaymentIntent capturePayment(UUID orderId) {
        log.info("Attempting to capture payment for orderId: {}", orderId);
        StripeChargeTransaction chargeTransaction = findChargeByOrderId(orderId);


        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(chargeTransaction.getPaymentIntentId());

            if (!paymentIntent.getStatus().equalsIgnoreCase(PaymentStates.REQUIRES_CAPTURE)) {
                throw new PaymentProcessingException("PaymentIntent cannot be captured. Status: " + paymentIntent.getStatus());
            }

            UUID idempotencyKey = UUID.randomUUID();
            RequestOptions requestOptions = RequestOptions.builder()
                    .setIdempotencyKey(idempotencyKey.toString())
                    .build();

            PaymentIntent capturedPaymentIntent = paymentIntent.capture(PaymentIntentCaptureParams.builder().build() , requestOptions);

            chargeTransaction.setStatus(capturedPaymentIntent.getStatus().toUpperCase());
            chargeTransaction.setIdempotencyKey(idempotencyKey);

            stripeChargeTransactionRepository.save(chargeTransaction);

            log.info("Successfully initiated capture for PaymentIntent {}", capturedPaymentIntent.getId());
            return capturedPaymentIntent;
        } catch (StripeException e) {
            log.error("Failed to capture PaymentIntent for orderId {}: {}", orderId, e.getMessage());
            throw new PaymentProcessingException("Stripe capture failed: " + e.getMessage());
        }
    }

    @Transactional
    public PaymentIntent cancelPayment(UUID orderId) {
        log.info("Attempting to cancel payment for orderId: {}", orderId);
        StripeChargeTransaction chargeTransaction = findChargeByOrderId(orderId);

        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(chargeTransaction.getPaymentIntentId());

            UUID idempotencyKey = UUID.randomUUID();
            RequestOptions requestOptions = RequestOptions.builder()
                    .setIdempotencyKey(idempotencyKey.toString())
                    .build();

            PaymentIntent canceledPaymentIntent = paymentIntent.cancel(PaymentIntentCancelParams.builder().build() , requestOptions);

            chargeTransaction.setIdempotencyKey(idempotencyKey);

            updateTransactionStatus(paymentIntent, canceledPaymentIntent.getStatus().toUpperCase());

            chargeTransaction.setStatus(paymentIntent.getStatus().toUpperCase());
            stripeChargeTransactionRepository.save(chargeTransaction);

            log.info("Successfully canceled PaymentIntent {}", canceledPaymentIntent.getId());
            return canceledPaymentIntent;
        } catch (StripeException e) {
            log.error("Failed to cancel PaymentIntent for orderId {}: {}", orderId, e.getMessage());
            throw new PaymentProcessingException("Stripe cancellation failed: " + e.getMessage());
        }
    }

    @Transactional
    public void handleStripeWebhook(String payload, String sigHeader) {
        log.info("Webhook triggered");
        Event event;
        try { /// For Security purpose
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Webhook error: {}", e.getMessage());
            throw new PaymentProcessingException("Invalid webhook signature or payload.");
        }

        if (event.getDataObjectDeserializer().getObject().orElse(null) instanceof PaymentIntent) {
            PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().get();
            String orderId = paymentIntent.getMetadata().get("orderId");
            String checkoutType = paymentIntent.getMetadata().get("checkoutType");
            switch (event.getType()) {
                case "payment_intent.created":
                    log.info("Webhook received: PaymentIntent {} created.", paymentIntent.getId());
                    String clientSecret = paymentIntent.getClientSecret();

                    var createdEvent = new PaymentIntentCreatedEvent(
                            paymentIntent.getId(),
                            clientSecret,
                            orderId
                    );

                    streamBridge.send("paymentIntentCreated-out-0", createdEvent);
                    log.info("Published PaymentIntentCreatedEvent {}", createdEvent);
                    break;
                case "payment_intent.succeeded":
                    log.info("Webhook received: PaymentIntent {} succeeded.", paymentIntent.getId());
                    finalizePaymentAndCreateTransfers(paymentIntent);


                    streamBridge.send("paymentSuccess-out-0", new PaymentSuccessEvent(paymentIntent.getId(), orderId, checkoutType));
                    log.info("Published PaymentSuccessEvent to Kafka for Order ID: {}", orderId);
                    break;
                case "payment_intent.requires_capture":
                    log.info("Webhook received: PaymentIntent {} requires capture.", paymentIntent.getId());
                    updateTransactionStatus(paymentIntent, "REQUIRES_CAPTURE");
                    break;
                case "payment_intent.amount_capturable_updated":
                    log.info("Webhook received: PaymentIntent {} amount_capturable_updated.", paymentIntent.getId());
                    PaymentAuthorizedEvent paymentAuthorizedEvent = new PaymentAuthorizedEvent(paymentIntent.getId(), orderId,checkoutType, new BigDecimal(paymentIntent.getAmount()));
                    streamBridge.send("paymentAuthorized-out-0", paymentAuthorizedEvent);
                    log.info("Published PaymentAuthorizedEvent to Kafka for Order ID: {}", orderId);
                    log.info("Payment Authorized Event Details{}", paymentAuthorizedEvent);
                    break;
                case "payment_intent.canceled":
                    log.info("Webhook received: PaymentIntent {} was canceled.", paymentIntent.getId());
                    updateTransactionStatus(paymentIntent, "CANCELED");
                    streamBridge.send("paymentFailed-out-0", new PaymentFailedEvent(paymentIntent.getId(),orderId, "Payment was canceled"));
                    log.info("Published PaymentFailedEvent to Kafka for Order ID: {}", orderId);
                    break;
                case "payment_intent.payment_failed":
                    String failureReason = paymentIntent.getLastPaymentError() != null ? paymentIntent.getLastPaymentError().getMessage() : "Unknown reason";
                    log.warn("Webhook received: PaymentIntent {} failed: {}", paymentIntent.getId(), paymentIntent.getLastPaymentError().getMessage());
                    updateTransactionStatus(paymentIntent, "FAILED");

                    streamBridge.send("paymentFailed-out-0", new PaymentFailedEvent(paymentIntent.getId(),orderId, failureReason));
                    log.info("Published PaymentFailedEvent to Kafka for Order ID: {}", orderId);
                    break;
                default:
                    log.warn("Unhandled event type for PaymentIntent: {}", event.getType());
            }
        } else {
            log.warn("Unhandled event type: {}", event.getType());
        }
    }

    private void finalizePaymentAndCreateTransfers(PaymentIntent paymentIntent) {
        StripeChargeTransaction chargeTransaction = stripeChargeTransactionRepository.findByPaymentIntentId(paymentIntent.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Charge Transaction", "paymentIntentId", paymentIntent.getId()));

        if ("SUCCEEDED".equals(paymentIntent.getStatus())) {
            log.warn("Received webhook for already succeeded PaymentIntent {}.", paymentIntent.getId());
            return;
        }

        chargeTransaction.setStatus("SUCCEEDED");
        chargeTransaction.setStripeChargeId(paymentIntent.getLatestCharge());

        stripeChargeTransactionRepository.save(chargeTransaction);

        Map<UUID, BigDecimal> sellerTotals = chargeTransaction.getOrderItems().stream()
                .collect(Collectors.groupingBy(OrderItem::getSellerId, Collectors.mapping(OrderItem::getAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));

        for (Map.Entry<UUID, BigDecimal> entry : sellerTotals.entrySet()) {
            UUID sellerId = entry.getKey();
            BigDecimal sellerAmount = entry.getValue();
            long sellerAmountInCents = sellerAmount.multiply(new BigDecimal("100")).longValue();

            try {
                String stripeAccountId = getStripeAccountId(sellerId);
                TransferCreateParams transferParams = TransferCreateParams.builder()
                        .setAmount(sellerAmountInCents)
                        .setCurrency(paymentIntent.getCurrency())
                        .setDestination(stripeAccountId)
                        .setSourceTransaction(paymentIntent.getLatestCharge())
                        .build();

                String idempotencyKey = "transfer-" + chargeTransaction.getId() + "-" + sellerId;
                RequestOptions requestOptions = RequestOptions.builder().setIdempotencyKey(idempotencyKey).build();


                Transfer transfer = Transfer.create(transferParams , requestOptions);
                StripeTransferTransaction transferTransaction = buildTransferTransaction(chargeTransaction, sellerId, sellerAmount, transfer.getId(), "SUCCEEDED", null);

                stripeTransferTransactionRepository.save(transferTransaction);
                log.info("Successfully transferred {} to seller {} for orderId {}", sellerAmount, sellerId, chargeTransaction.getOrderId());

            } catch (StripeException | ResourceNotFoundException e) {
                log.error("Failed to transfer funds to seller {} for orderId {}: {}", sellerId, chargeTransaction.getOrderId(), e.getMessage());
                StripeTransferTransaction transferTransaction = buildTransferTransaction(chargeTransaction, sellerId, sellerAmount, null, "FAILED", e.getMessage());
                stripeTransferTransactionRepository.save(transferTransaction);
            }
        }
    }

    private void updateTransactionStatus(PaymentIntent paymentIntent, String status) {
        stripeChargeTransactionRepository.findByPaymentIntentId(paymentIntent.getId()).ifPresent(charge -> {
            charge.setStatus(status);

            if (status.equalsIgnoreCase(PaymentStates.FAILED) && paymentIntent.getLastPaymentError() != null)
                charge.setStripeErrorMessage(paymentIntent.getLastPaymentError().getMessage());

            stripeChargeTransactionRepository.save(charge);
        });
    }

    @Transactional
    public RefundResponse refundPayment(RefundRequest refundRequest) {
        log.info("Processing refund for orderId: {} with idempotency key: {}", refundRequest.orderId(), refundRequest.idempotencyKey());

        StripeChargeTransaction chargeTransaction = findChargeByOrderId(refundRequest.orderId());

        if (chargeTransaction.getRefunded().equals(Boolean.TRUE))
            throw new PaymentProcessingException("This transaction has already been refunded");
        if (!chargeTransaction.getStatus().equals(PaymentStates.SUCCEEDED))
            throw new PaymentProcessingException("Cannot refund a transaction that has not been captured and succeeded");

        try {
            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(chargeTransaction.getPaymentIntentId())
                    .build();

            RequestOptions requestOptions = RequestOptions.builder().setIdempotencyKey(refundRequest.idempotencyKey().toString()).build();
            Refund refund = Refund.create(params, requestOptions);

            chargeTransaction.setStatus(PaymentStates.REFUNDED);
            chargeTransaction.setRefunded(true);
            stripeChargeTransactionRepository.save(chargeTransaction);

            for (StripeTransferTransaction transfer : chargeTransaction.getTransfers()) {
                transfer.setStatus(PaymentStates.REVERSED);
                stripeTransferTransactionRepository.save(transfer);
            }

            log.info("Successfully refunded PaymentIntent {} for orderId {}. Refund ID: {}", chargeTransaction.getPaymentIntentId(), refundRequest.orderId(), refund.getId());
            return new RefundResponse(refund.getId(), refundRequest.orderId(), refund.getStatus(), "Full refund processed successfully.");

        } catch (StripeException e) {
            log.error("Failed to refund PaymentIntent for orderId {}: {}", refundRequest.orderId(), e.getMessage());
            throw new PaymentProcessingException("Stripe refund failed: " + e.getMessage());
        }
    }

    private StripeChargeTransaction buildChargeTransaction(CreatePaymentIntentRequest request, BigDecimal totalAmount, PaymentIntent paymentIntent, UUID idempotencyKey) {
        StripeChargeTransaction charge = StripeChargeTransaction.builder()
                .orderId(request.orderId())
                .paymentIntentId(paymentIntent.getId())
                .amount(totalAmount)
                .idempotencyKey(idempotencyKey)
                .currency(request.currency())
                .status(paymentIntent.getStatus().toUpperCase())
                .refunded(false)
                .build();

        List<OrderItem> orderItems = request.items().stream()
                .map(itemDto -> OrderItem.builder()
                        .chargeTransaction(charge)
                        .sellerId(itemDto.sellerId())
                        .amount(itemDto.amount())
                        .build())
                .collect(Collectors.toList());

        charge.setOrderItems(orderItems);
        return charge;
    }

    private StripeTransferTransaction buildTransferTransaction(StripeChargeTransaction parent, UUID sellerId, BigDecimal amount, String stripeTransferId, String status, String stripeErrorMessage) {
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

    public String getStripeAccountId(UUID sellerId) {
        SellerStripeAccount sellerStripeAccount = sellerStripeAccountRepository.findBySellerId(sellerId);
        if (sellerStripeAccount == null)
            throw new ResourceNotFoundException("SellerStripeAccount", "sellerId", sellerId.toString());

        return sellerStripeAccount.getStripeAccountId();
    }

    public StripeChargeTransaction findChargeByOrderId(UUID orderId) {
        return stripeChargeTransactionRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Charge Transaction", "orderId", orderId.toString()));
    }
}
