package com.mazadak.payment.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/*TODO:
   * Create /transactions , /transactions/{orderId} endpoints
* */

@Entity
@Table(name = "stripe_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StripeTransaction extends BaseEntity {

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;


    @Column(nullable = false)
    private String orderId;

    private String stripeChargeId;

    @Column(nullable = false)
    private String sellerStripeAccountId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private BigDecimal commission;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String idempotencyKey;

    private String stripeErrorMessage;

}