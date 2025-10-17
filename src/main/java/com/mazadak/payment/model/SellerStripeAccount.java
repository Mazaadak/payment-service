package com.mazadak.payment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "seller_stripe_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerStripeAccount extends BaseEntity {

    @Id
    private UUID sellerId;

    @Column(nullable = false, unique = true)
    private String stripeAccountId;
}