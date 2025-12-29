-- ============================================================================
-- V4: Create Merchant Alert Thresholds Table
-- ============================================================================
-- This table stores custom alert thresholds per merchant
-- Allows different monitoring configurations for different merchants
-- ============================================================================

CREATE TABLE IF NOT EXISTS merchant_alert_thresholds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- Merchant identification
    merch_id VARCHAR(20) NOT NULL,
    hsn VARCHAR(50),
    merchant_name VARCHAR(100),

    -- Error rate thresholds (as decimal, e.g., 0.05 = 5%)
    error_rate_warning_threshold DOUBLE DEFAULT 0.02,
    error_rate_critical_threshold DOUBLE DEFAULT 0.05,

    -- Processing time thresholds (in milliseconds)
    processing_time_warning_ms BIGINT DEFAULT 5000,
    processing_time_critical_ms BIGINT DEFAULT 10000,

    -- Volume thresholds
    min_daily_batches INT DEFAULT 10,
    max_daily_errors INT DEFAULT 100,

    -- Risk score thresholds
    risk_score_warning_threshold DOUBLE DEFAULT 0.4,
    risk_score_critical_threshold DOUBLE DEFAULT 0.7,

    -- Alert configuration
    alerts_enabled BOOLEAN DEFAULT TRUE,
    email_notification_enabled BOOLEAN DEFAULT FALSE,
    slack_notification_enabled BOOLEAN DEFAULT FALSE,
    notification_email VARCHAR(255),
    notification_slack_channel VARCHAR(100),

    -- Priority level (higher = more important)
    priority_level INT DEFAULT 1,

    -- Notes
    notes VARCHAR(500),

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),

    -- Unique constraint on merchant ID and HSN combination
    CONSTRAINT uk_merchant_hsn UNIQUE (merch_id, hsn)
);

-- Create indexes for common queries
CREATE INDEX idx_merchant_thresholds_merch_id ON merchant_alert_thresholds(merch_id);
CREATE INDEX idx_merchant_thresholds_alerts_enabled ON merchant_alert_thresholds(alerts_enabled);
CREATE INDEX idx_merchant_thresholds_priority ON merchant_alert_thresholds(priority_level);

-- Insert some sample threshold configurations for testing
INSERT INTO merchant_alert_thresholds (
    merch_id, hsn, merchant_name,
    error_rate_warning_threshold, error_rate_critical_threshold,
    processing_time_warning_ms, processing_time_critical_ms,
    priority_level, notes
) VALUES
(
    'MERCH001', 'HSN001', 'High Volume Merchant',
    0.01, 0.03,  -- Stricter thresholds
    3000, 7000,  -- Faster response expected
    5, 'High-priority merchant with strict SLA requirements'
),
(
    'MERCH002', 'HSN002', 'Standard Merchant',
    0.02, 0.05,  -- Default thresholds
    5000, 10000,
    3, 'Standard merchant with default thresholds'
),
(
    'MERCH003', 'HSN003', 'Low Volume Merchant',
    0.05, 0.10,  -- More lenient thresholds
    10000, 20000,
    1, 'Low-volume merchant with relaxed thresholds'
);

