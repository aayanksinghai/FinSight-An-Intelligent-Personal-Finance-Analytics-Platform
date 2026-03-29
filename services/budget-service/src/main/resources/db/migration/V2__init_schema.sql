CREATE TABLE budgets (
    id UUID PRIMARY KEY,
    owner_email VARCHAR(255) NOT NULL,
    category_id VARCHAR(100) NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    limit_amount DECIMAL(15, 2) NOT NULL,
    current_spend DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    month_year VARCHAR(7) NOT NULL, -- Format: YYYY-MM
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_owner_category_month UNIQUE (owner_email, category_id, month_year)
);

CREATE INDEX idx_budgets_owner_month ON budgets(owner_email, month_year);
