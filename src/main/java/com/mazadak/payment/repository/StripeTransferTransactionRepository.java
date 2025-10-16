package com.mazadak.payment.repository;

import com.mazadak.payment.model.StripeTransferTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface StripeTransferTransactionRepository extends JpaRepository<StripeTransferTransaction, UUID> {
}
