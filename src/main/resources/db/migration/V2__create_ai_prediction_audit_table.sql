-- Create AI prediction audit table
CREATE TABLE IF NOT EXISTS ai_prediction_audit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prediction_time TIMESTAMP NOT NULL,
    predicted_status VARCHAR(20) NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    time_horizon_hours INTEGER NOT NULL,
    reasoning TEXT,
    input_metrics_count INTEGER,
    prompt_version VARCHAR(10),
    actual_outcome VARCHAR(20),
    outcome_timestamp TIMESTAMP,
    is_correct BOOLEAN,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create table for key findings
CREATE TABLE IF NOT EXISTS ai_prediction_findings (
    prediction_id BIGINT NOT NULL REFERENCES ai_prediction_audit(id) ON DELETE CASCADE,
    finding TEXT NOT NULL,
    CONSTRAINT pk_prediction_findings PRIMARY KEY (prediction_id, finding)
);

-- Create table for recommendations
CREATE TABLE IF NOT EXISTS ai_prediction_recommendations (
    prediction_id BIGINT NOT NULL REFERENCES ai_prediction_audit(id) ON DELETE CASCADE,
    recommendation TEXT NOT NULL,
    CONSTRAINT pk_prediction_recommendations PRIMARY KEY (prediction_id, recommendation)
);

-- Create indexes
CREATE INDEX idx_ai_prediction_time ON ai_prediction_audit(prediction_time);
CREATE INDEX idx_ai_prediction_status ON ai_prediction_audit(predicted_status);
CREATE INDEX idx_ai_prediction_confidence ON ai_prediction_audit(confidence);
CREATE INDEX idx_ai_prediction_actual_outcome ON ai_prediction_audit(actual_outcome);
CREATE INDEX idx_ai_prediction_is_correct ON ai_prediction_audit(is_correct);
