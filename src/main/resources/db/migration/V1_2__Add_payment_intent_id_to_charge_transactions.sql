ALTER TABLE charge_stripe_transactions
ADD COLUMN payment_intent_id VARCHAR(255) UNIQUE;
