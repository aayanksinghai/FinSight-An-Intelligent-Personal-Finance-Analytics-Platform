CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    owner_email VARCHAR(320) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    type VARCHAR(20) NOT NULL,
    category VARCHAR(60) NOT NULL,
    amount NUMERIC(14,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    description VARCHAR(255),
    merchant VARCHAR(120),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_transactions_owner_occurred_at ON transactions (owner_email, occurred_at DESC);
CREATE INDEX idx_transactions_owner_category ON transactions (owner_email, category);

