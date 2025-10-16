package com.mazadak.payment.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "charge_stripe_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StripeChargeTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String orderId;

    @Column(unique = true)
    private String stripeChargeId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String status;

    @Column(unique = true)
    private String idempotencyKey;

    private String stripeErrorMessage;

    private Boolean refunded;

    @OneToMany(mappedBy = "chargeTransaction", cascade = CascadeType.ALL)
    private List<StripeTransferTransaction> transfers;
}