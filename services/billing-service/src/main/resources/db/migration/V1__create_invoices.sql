CREATE TABLE invoices (
    invoice_id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    amount BIGINT NOT NULL CHECK (amount >= 0),
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL CHECK (status IN ('DRAFT','OPEN','PAID','VOID','UNCOLLECTIBLE','FAILED')),
    stripe_invoice_id VARCHAR(255),
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_invoices_user_id ON invoices(user_id);
