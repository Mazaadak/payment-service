package com.mazadak.payment.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;


@Entity
@Table(name = "transfer_stripe_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StripeTransferTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "charge_transaction_id", nullable = false)
    @JsonIgnore
    private StripeChargeTransaction chargeTransaction;

    @Column(unique = true)
    private String stripeTransferId;

    @Column(nullable = false)
    private String sellerStripeAccountId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String status;

    private String stripeErrorMessage;

}