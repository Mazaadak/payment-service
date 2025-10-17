CREATE TABLE charge_transactions
(
    id                   UUID PRIMARY KEY,
    order_id             UUID   NOT NULL UNIQUE,
    stripe_charge_id     VARCHAR(255) UNIQUE,
    amount               NUMERIC(19, 2) NOT NULL,
    currency             VARCHAR(255)   NOT NULL,
    status               VARCHAR(255)   NOT NULL,
    idempotency_key      UUID UNIQUE,
    stripe_error_message TEXT,
    refunded             BOOLEAN,
    created_at           TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    created_by           VARCHAR(255),
    updated_by           VARCHAR(255)
);

CREATE TABLE transfer_transactions
(
    id                       UUID PRIMARY KEY,
    charge_transaction_id    UUID           NOT NULL REFERENCES charge_transactions (id),
    stripe_transfer_id       VARCHAR(255) UNIQUE,
    seller_stripe_account_id VARCHAR(255)   NOT NULL,
    amount                   NUMERIC(19, 2) NOT NULL,
    currency                 VARCHAR(255)   NOT NULL,
    status                   VARCHAR(255)   NOT NULL,
    stripe_error_message     TEXT,
    created_at               TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    created_by               VARCHAR(255),
    updated_by               VARCHAR(255)
);

CREATE TABLE seller_stripe_accounts
(
    sellerId                 UUID PRIMARY KEY,
    stripeAccountId          VARCHAR(255) UNIQUE NOT NULL,
    created_at               TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    created_by               VARCHAR(255),
    updated_by               VARCHAR(255)
);
