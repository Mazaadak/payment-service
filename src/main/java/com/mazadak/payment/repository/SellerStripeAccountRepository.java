package com.mazadak.payment.repository;

import com.mazadak.payment.model.SellerStripeAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SellerStripeAccountRepository extends JpaRepository<SellerStripeAccount, String> {
}