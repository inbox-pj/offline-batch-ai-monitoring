-- ============================================================================
-- V5: Add A/B Testing and Enhanced Tracking Fields to AI Prediction Audit
-- ============================================================================
-- Adds fields for A/B testing, merchant tracking, response times, and error tracking
-- ============================================================================

-- Add A/B testing columns
ALTER TABLE ai_prediction_audit ADD COLUMN IF NOT EXISTS model_type VARCHAR(20) DEFAULT 'AI';
ALTER TABLE ai_prediction_audit ADD COLUMN IF NOT EXISTS is_ab_test BOOLEAN DEFAULT FALSE;
ALTER TABLE ai_prediction_audit ADD COLUMN IF NOT EXISTS ab_test_group VARCHAR(50);

-- Add enhanced tracking columns
ALTER TABLE ai_prediction_audit ADD COLUMN IF NOT EXISTS merch_id VARCHAR(20);
ALTER TABLE ai_prediction_audit ADD COLUMN IF NOT EXISTS response_time_ms BIGINT;
ALTER TABLE ai_prediction_audit ADD COLUMN IF NOT EXISTS is_error BOOLEAN DEFAULT FALSE;
ALTER TABLE ai_prediction_audit ADD COLUMN IF NOT EXISTS error_message VARCHAR(500);
ALTER TABLE ai_prediction_audit ADD COLUMN IF NOT EXISTS risk_score DOUBLE PRECISION;
ALTER TABLE ai_prediction_audit ADD COLUMN IF NOT EXISTS actual_error_rate DOUBLE PRECISION;
ALTER TABLE ai_prediction_audit ADD COLUMN IF NOT EXISTS predicted_error_rate DOUBLE PRECISION;
ALTER TABLE ai_prediction_audit ADD COLUMN IF NOT EXISTS evaluation_notes VARCHAR(500);

-- Create indexes for new columns
CREATE INDEX IF NOT EXISTS idx_ai_prediction_model_type ON ai_prediction_audit(model_type);
CREATE INDEX IF NOT EXISTS idx_ai_prediction_ab_test ON ai_prediction_audit(is_ab_test);
CREATE INDEX IF NOT EXISTS idx_ai_prediction_ab_test_group ON ai_prediction_audit(ab_test_group);
CREATE INDEX IF NOT EXISTS idx_ai_prediction_merch_id ON ai_prediction_audit(merch_id);
CREATE INDEX IF NOT EXISTS idx_ai_prediction_is_error ON ai_prediction_audit(is_error);

-- Create accuracy metrics summary table for historical tracking
CREATE TABLE IF NOT EXISTS accuracy_metrics_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    calculation_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    analysis_window_days INTEGER NOT NULL,

    -- Overall metrics
    total_predictions BIGINT,
    evaluated_predictions BIGINT,
    correct_predictions BIGINT,
    overall_accuracy DOUBLE PRECISION,
    average_confidence DOUBLE PRECISION,

    -- Weighted metrics
    weighted_precision DOUBLE PRECISION,
    weighted_recall DOUBLE PRECISION,
    weighted_f1_score DOUBLE PRECISION,

    -- Macro metrics
    macro_precision DOUBLE PRECISION,
    macro_recall DOUBLE PRECISION,
    macro_f1_score DOUBLE PRECISION,

    -- Calibration and trend
    confidence_calibration DOUBLE PRECISION,
    accuracy_trend DOUBLE PRECISION,

    -- A/B test summary
    ai_accuracy DOUBLE PRECISION,
    rule_based_accuracy DOUBLE PRECISION,
    winner VARCHAR(20),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_accuracy_history_time ON accuracy_metrics_history(calculation_time);

-- Create table for tracking model drift
CREATE TABLE IF NOT EXISTS model_drift_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    detected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    drift_type VARCHAR(50) NOT NULL,
    drift_score DOUBLE PRECISION,
    drift_start_date TIMESTAMP,
    description TEXT,
    baseline_accuracy DOUBLE PRECISION,
    current_accuracy DOUBLE PRECISION,
    is_resolved BOOLEAN DEFAULT FALSE,
    resolved_at TIMESTAMP,
    resolution_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_drift_events_detected ON model_drift_events(detected_at);
CREATE INDEX IF NOT EXISTS idx_drift_events_type ON model_drift_events(drift_type);
CREATE INDEX IF NOT EXISTS idx_drift_events_resolved ON model_drift_events(is_resolved);

-- Insert sample data for testing A/B comparison
-- AI model predictions
INSERT INTO ai_prediction_audit (
    prediction_time, predicted_status, confidence, time_horizon_hours,
    reasoning, input_metrics_count, prompt_version, model_type,
    actual_outcome, outcome_timestamp, is_correct
) VALUES
(DATEADD('HOUR', -48, CURRENT_TIMESTAMP), 'HEALTHY', 0.92, 6, 'AI: Normal metrics', 100, 'v2', 'AI', 'HEALTHY', DATEADD('HOUR', -42, CURRENT_TIMESTAMP), TRUE),
(DATEADD('HOUR', -36, CURRENT_TIMESTAMP), 'WARNING', 0.85, 6, 'AI: Elevated errors', 100, 'v2', 'AI', 'WARNING', DATEADD('HOUR', -30, CURRENT_TIMESTAMP), TRUE),
(DATEADD('HOUR', -24, CURRENT_TIMESTAMP), 'HEALTHY', 0.88, 6, 'AI: Normal metrics', 100, 'v2', 'AI', 'WARNING', DATEADD('HOUR', -18, CURRENT_TIMESTAMP), FALSE),
(DATEADD('HOUR', -12, CURRENT_TIMESTAMP), 'CRITICAL', 0.78, 6, 'AI: High error rate', 100, 'v2', 'AI', 'CRITICAL', DATEADD('HOUR', -6, CURRENT_TIMESTAMP), TRUE);

-- Rule-based predictions
INSERT INTO ai_prediction_audit (
    prediction_time, predicted_status, confidence, time_horizon_hours,
    reasoning, input_metrics_count, prompt_version, model_type,
    actual_outcome, outcome_timestamp, is_correct
) VALUES
(DATEADD('HOUR', -47, CURRENT_TIMESTAMP), 'HEALTHY', 0.70, 6, 'Rule-based: Low error rate', 100, 'v1', 'RULE_BASED', 'HEALTHY', DATEADD('HOUR', -41, CURRENT_TIMESTAMP), TRUE),
(DATEADD('HOUR', -35, CURRENT_TIMESTAMP), 'WARNING', 0.70, 6, 'Rule-based: Warning threshold exceeded', 100, 'v1', 'RULE_BASED', 'WARNING', DATEADD('HOUR', -29, CURRENT_TIMESTAMP), TRUE),
(DATEADD('HOUR', -23, CURRENT_TIMESTAMP), 'HEALTHY', 0.70, 6, 'Rule-based: Low error rate', 100, 'v1', 'RULE_BASED', 'WARNING', DATEADD('HOUR', -17, CURRENT_TIMESTAMP), FALSE),
(DATEADD('HOUR', -11, CURRENT_TIMESTAMP), 'WARNING', 0.70, 6, 'Rule-based: Warning threshold', 100, 'v1', 'RULE_BASED', 'CRITICAL', DATEADD('HOUR', -5, CURRENT_TIMESTAMP), FALSE);

