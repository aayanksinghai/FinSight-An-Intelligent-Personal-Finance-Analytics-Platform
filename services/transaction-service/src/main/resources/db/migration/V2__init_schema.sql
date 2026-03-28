CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    parent_id UUID REFERENCES categories(id),
    icon VARCHAR(50),
    color VARCHAR(20),
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    owner_email VARCHAR(255) NOT NULL,
    account_id UUID, -- For future multi-account support
    job_id VARCHAR(255), -- Reference to the ingestion job
    source_file_name VARCHAR(255),
    source_bank VARCHAR(100),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    raw_description TEXT NOT NULL,
    normalized_merchant VARCHAR(255),
    category_id UUID REFERENCES categories(id),
    amount NUMERIC(15, 2) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('DEBIT', 'CREDIT')),
    currency VARCHAR(10) DEFAULT 'INR',
    balance_after NUMERIC(15, 2),
    raw_text TEXT,
    tags TEXT[], -- Array of strings for custom user tags
    is_anomaly BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes for frequent queries (dashboard aggregations)
CREATE INDEX idx_transactions_owner_email ON transactions(owner_email);
CREATE INDEX idx_transactions_occurred_at ON transactions(occurred_at);
CREATE INDEX idx_transactions_category_id ON transactions(category_id);

-- Insert common financial categories
INSERT INTO categories (id, name, icon, color, description) VALUES
    ('00000000-0000-0000-0000-000000000001', 'Income', '💰', '#10B981', 'Salary, deposits, and other income'),
    ('00000000-0000-0000-0000-000000000002', 'Food & Dining', '🍔', '#F59E0B', 'Restaurants, groceries, coffee shops'),
    ('00000000-0000-0000-0000-000000000003', 'Shopping', '🛍️', '#EC4899', 'Clothing, electronics, online shopping'),
    ('00000000-0000-0000-0000-000000000004', 'Housing', '🏠', '#8B5CF6', 'Rent, mortgage, maintenance'),
    ('00000000-0000-0000-0000-000000000005', 'Transportation', '🚗', '#3B82F6', 'Gas, public transit, car payments, tolls'),
    ('00000000-0000-0000-0000-000000000006', 'Utilities', '⚡', '#14B8A6', 'Electricity, water, internet, phone'),
    ('00000000-0000-0000-0000-000000000007', 'Health & Fitness', '⚕️', '#EF4444', 'Pharmacy, gym, doctor, insurance'),
    ('00000000-0000-0000-0000-000000000008', 'Entertainment', '🎬', '#F43F5E', 'Movies, concerts, streaming, hobbies'),
    ('00000000-0000-0000-0000-000000000009', 'Travel', '✈️', '#0EA5E9', 'Flights, hotels, vacation expenses'),
    ('00000000-0000-0000-0000-000000000010', 'Transfer', '🔄', '#64748B', 'Transfers between accounts'),
    ('00000000-0000-0000-0000-000000000011', 'Uncategorized', '❓', '#94A3B8', 'Transactions not yet categorized');
