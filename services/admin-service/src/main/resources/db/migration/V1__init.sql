CREATE TABLE IF NOT EXISTS admin_config (
    config_key VARCHAR(255) PRIMARY KEY,
    config_value VARCHAR(255) NOT NULL,
    description TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seed default ML model thresholds
INSERT INTO admin_config (config_key, config_value, description) VALUES
('anomaly.sensitivity', '1.5', 'Multiplier for standard deviation to trigger anomaly'),
('stress.weight.spend', '0.40', 'Weight for spend-to-income ratio in stress score'),
('stress.weight.savings', '0.20', 'Weight for savings rate in stress score'),
('stress.weight.recurring', '0.15', 'Weight for recurring burden in stress score'),
('stress.weight.discretionary', '0.15', 'Weight for discretionary growth in stress score'),
('stress.weight.adherence', '0.10', 'Weight for budget adherence in stress score')
ON CONFLICT (config_key) DO NOTHING;
