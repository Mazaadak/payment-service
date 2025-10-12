CREATE TABLE stripe_transactions (
                              id UUID PRIMARY KEY,

                              order_id VARCHAR(255) NOT NULL,

                              stripe_charge_id VARCHAR(255),

                              seller_stripe_account_id VARCHAR(255) NOT NULL,

                              amount NUMERIC(19, 2) NOT NULL,

                              commission NUMERIC(19, 2) NOT NULL,

                              currency VARCHAR(3) NOT NULL,

                              status VARCHAR(50) NOT NULL,

                              idempotency_key VARCHAR(255) NOT NULL UNIQUE,

                              stripe_error_message TEXT,

                              created_by VARCHAR(255) NOT NULL,
                              updated_by VARCHAR(255),
                              created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
                              updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_transactions_order_id ON transactions(order_id);