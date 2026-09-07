-- The card a driver saves, and what we charged them for a session.

CREATE TABLE billing_customers (
    user_id VARCHAR(255) PRIMARY KEY,
    stripe_customer_id VARCHAR(255) NOT NULL UNIQUE,
    default_payment_method_id VARCHAR(255),
    card_brand VARCHAR(40),
    card_last4 VARCHAR(4),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Settlement is driven by an at-least-once event, so the same session can arrive
-- twice. session_id is the natural idempotency key: one charge per session, and a
-- replay collides here instead of billing the driver again.
ALTER TABLE invoices
    ADD COLUMN session_id VARCHAR(255),
    ADD COLUMN stripe_payment_intent_id VARCHAR(255),
    ADD COLUMN failure_reason TEXT;

CREATE UNIQUE INDEX uk_invoices_session ON invoices (session_id) WHERE session_id IS NOT NULL;
