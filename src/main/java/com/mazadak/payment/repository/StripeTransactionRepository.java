package com.mazadak.payment.repository;

import com.mazadak.payment.model.StripeTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StripeTransactionRepository extends JpaRepository<StripeTransaction, UUID> {

    Optional<StripeTransaction> findByOrderId(String orderId);
}
