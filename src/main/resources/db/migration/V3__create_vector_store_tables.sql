-- Enable pgvector extension (if using PostgreSQL with pgvector)
-- Commented out for H2 compatibility
-- CREATE EXTENSION IF NOT EXISTS vector;

-- Create historical patterns table for vector storage (H2 compatible)
CREATE TABLE IF NOT EXISTS ai_historical_patterns (
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    content TEXT NOT NULL,
    metadata_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Note: For PostgreSQL with pgvector, add vector column:
-- ALTER TABLE ai_historical_patterns ADD COLUMN embedding vector(384);
-- CREATE INDEX ON ai_historical_patterns USING ivfflat (embedding vector_cosine_ops);

CREATE INDEX idx_historical_patterns_created_at ON ai_historical_patterns(created_at);

-- Comments removed for H2 compatibility
-- COMMENT ON TABLE ai_historical_patterns IS 'Stores historical patterns for RAG';
-- COMMENT ON COLUMN ai_historical_patterns.content IS 'Text content of the pattern';
-- COMMENT ON COLUMN ai_historical_patterns.metadata_json IS 'Additional metadata as JSON';
