package com.mazadak.payment.repository;

import com.mazadak.payment.model.StripeChargeTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StripeChargeTransactionRepository extends JpaRepository<StripeChargeTransaction, UUID> {

    Optional<StripeChargeTransaction> findByOrderId(String orderId);
    Optional<StripeChargeTransaction> findByPaymentIntentId(String paymentIntentId);
}
