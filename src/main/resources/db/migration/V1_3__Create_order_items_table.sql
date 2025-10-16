CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    charge_transaction_id UUID NOT NULL REFERENCES charge_stripe_transactions(id),
    seller_id VARCHAR(255) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE INDEX idx_order_items_charge_id ON order_items(charge_transaction_id);
