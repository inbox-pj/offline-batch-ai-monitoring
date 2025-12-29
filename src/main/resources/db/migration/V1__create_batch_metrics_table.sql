-- Create offline_batch_metrics table
CREATE TABLE IF NOT EXISTS offline_batch_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    merch_id VARCHAR(20) NOT NULL,
    hsn VARCHAR(50) NOT NULL,
    batch_id VARCHAR(50),
    processed_count BIGINT NOT NULL DEFAULT 0,
    error_count BIGINT NOT NULL DEFAULT 0,
    processing_time_ms BIGINT NOT NULL DEFAULT 0,
    batch_size INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(50),
    error_type VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX idx_batch_metrics_timestamp ON offline_batch_metrics(timestamp);
CREATE INDEX idx_batch_metrics_merch_hsn ON offline_batch_metrics(merch_id, hsn);
CREATE INDEX idx_batch_metrics_status ON offline_batch_metrics(status);
CREATE INDEX idx_batch_metrics_error_count ON offline_batch_metrics(error_count);
CREATE INDEX idx_batch_metrics_created_at ON offline_batch_metrics(created_at);
