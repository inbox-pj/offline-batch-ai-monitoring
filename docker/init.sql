-- Initialize database for offline batch AI monitoring
CREATE DATABASE IF NOT EXISTS offline_batch_ai;

\c offline_batch_ai;

-- Create extension for PostgreSQL (if needed)
-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
-- CREATE EXTENSION IF NOT EXISTS vector;

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE offline_batch_ai TO postgres;
