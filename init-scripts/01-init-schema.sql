-- ============================================
-- Database Schema for Workflow Automation Platform
-- ============================================

-- Create pgvector extension for RAG/AI embeddings
CREATE EXTENSION IF NOT EXISTS vector;

-- ============================================
-- Workflows Table
-- ============================================
CREATE TABLE IF NOT EXISTS workflows (
    workflow_id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(500) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    nodes JSONB NOT NULL,
    edges JSONB NOT NULL,
    status VARCHAR(50) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'PAUSED', 'ARCHIVED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index for faster user workflow queries
CREATE INDEX idx_workflows_user_id ON workflows(user_id);
CREATE INDEX idx_workflows_status ON workflows(status);

-- ============================================
-- Execution History Table
-- ============================================
CREATE TABLE IF NOT EXISTS execution_history (
    execution_id VARCHAR(255) PRIMARY KEY,
    workflow_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL CHECK (status IN ('RUNNING', 'SUCCESS', 'FAILED')),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    triggered_by VARCHAR(100),
    node_results JSONB,
    error_message TEXT,
    FOREIGN KEY (workflow_id) REFERENCES workflows(workflow_id) ON DELETE CASCADE
);

-- Indexes for faster queries
CREATE INDEX idx_execution_history_workflow_id ON execution_history(workflow_id);
CREATE INDEX idx_execution_history_user_id ON execution_history(user_id);
CREATE INDEX idx_execution_history_start_time ON execution_history(start_time DESC);

-- ============================================
-- Users Table
-- ============================================
CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(500) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

CREATE INDEX idx_users_email ON users(email);

-- ============================================
-- Credentials Table (Encrypted Storage)
-- ============================================
CREATE TABLE IF NOT EXISTS credentials (
    credential_id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    service_type VARCHAR(100) NOT NULL, -- 'gmail', 'gemini', etc.
    credential_name VARCHAR(255) NOT NULL,
    encrypted_data TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    UNIQUE(user_id, service_type, credential_name)
);

CREATE INDEX idx_credentials_user_id ON credentials(user_id);
CREATE INDEX idx_credentials_service_type ON credentials(service_type);

-- ============================================
-- Triggers (Scheduled Jobs)
-- ============================================
CREATE TABLE IF NOT EXISTS triggers (
    trigger_id VARCHAR(255) PRIMARY KEY,
    workflow_id VARCHAR(255) NOT NULL,
    trigger_type VARCHAR(50) NOT NULL CHECK (trigger_type IN ('cron', 'webhook', 'manual', 'event')),
    cron_expression VARCHAR(100),
    webhook_url VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    last_triggered_at TIMESTAMP,
    next_trigger_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (workflow_id) REFERENCES workflows(workflow_id) ON DELETE CASCADE
);

CREATE INDEX idx_triggers_workflow_id ON triggers(workflow_id);
CREATE INDEX idx_triggers_next_trigger_at ON triggers(next_trigger_at) WHERE is_active = TRUE;

-- ============================================
-- Execution Logs Table
-- ============================================
CREATE TABLE IF NOT EXISTS execution_logs (
    log_id SERIAL PRIMARY KEY,
    execution_id VARCHAR(255) NOT NULL,
    node_id VARCHAR(255),
    log_level VARCHAR(20) NOT NULL CHECK (log_level IN ('INFO', 'WARN', 'ERROR', 'DEBUG')),
    message TEXT NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (execution_id) REFERENCES execution_history(execution_id) ON DELETE CASCADE
);

CREATE INDEX idx_execution_logs_execution_id ON execution_logs(execution_id);
CREATE INDEX idx_execution_logs_created_at ON execution_logs(created_at DESC);

-- ============================================
-- Vector Embeddings Table (for RAG/AI)
-- ============================================
CREATE TABLE IF NOT EXISTS document_embeddings (
    embedding_id SERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    document_id VARCHAR(255) NOT NULL,
    chunk_text TEXT NOT NULL,
    embedding vector(768), -- 768 dimensions for Gemini embeddings
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Index for vector similarity search
CREATE INDEX idx_embeddings_vector ON document_embeddings USING ivfflat (embedding vector_cosine_ops);
CREATE INDEX idx_embeddings_user_id ON document_embeddings(user_id);

-- ============================================
-- Audit Trail Table
-- ============================================
CREATE TABLE IF NOT EXISTS audit_trail (
    audit_id SERIAL PRIMARY KEY,
    user_id VARCHAR(255),
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100) NOT NULL, -- 'workflow', 'credential', 'user'
    resource_id VARCHAR(255),
    changes JSONB,
    ip_address VARCHAR(50),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_trail_user_id ON audit_trail(user_id);
CREATE INDEX idx_audit_trail_created_at ON audit_trail(created_at DESC);

-- ============================================
-- Function: Update updated_at timestamp
-- ============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to tables
CREATE TRIGGER update_workflows_updated_at BEFORE UPDATE ON workflows
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_credentials_updated_at BEFORE UPDATE ON credentials
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- Insert Sample Data (for testing)
-- ============================================

-- Sample User
INSERT INTO users (user_id, email, password_hash, first_name, last_name)
VALUES 
    ('user-123', 'test@workflow.com', '$2a$10$abcdefghijklmnopqrstuvwxyz', 'Test', 'User')
ON CONFLICT (user_id) DO NOTHING;

-- Sample Workflow
INSERT INTO workflows (workflow_id, name, user_id, nodes, edges, status)
VALUES (
    'workflow-sample-1',
    'Daily Email Summary',
    'user-123',
    '[
        {
            "id": "node-1",
            "type": "trigger",
            "position": {"x": 100, "y": 200},
            "config": {
                "triggerType": "cron",
                "cronExpression": "0 8 * * *"
            }
        },
        {
            "id": "node-2",
            "type": "gmail",
            "position": {"x": 300, "y": 200},
            "config": {
                "action": "read",
                "filters": {
                    "from": "client@example.com",
                    "isUnread": true
                }
            }
        },
        {
            "id": "node-3",
            "type": "gemini",
            "position": {"x": 500, "y": 200},
            "config": {
                "prompt": "Summarize this email: {{node-2.output.body}}",
                "model": "gemini-pro"
            }
        },
        {
            "id": "node-4",
            "type": "http",
            "position": {"x": 700, "y": 200},
            "config": {
                "method": "POST",
                "url": "https://webhook.site/test",
                "body": {
                    "summary": "{{node-3.output.text}}",
                    "originalSubject": "{{node-2.output.subject}}"
                }
            }
        }
    ]'::jsonb,
    '[
        {"from": "node-1", "to": "node-2"},
        {"from": "node-2", "to": "node-3"},
        {"from": "node-3", "to": "node-4"}
    ]'::jsonb,
    'ACTIVE'
)
ON CONFLICT (workflow_id) DO NOTHING;

-- Sample Trigger
INSERT INTO triggers (trigger_id, workflow_id, trigger_type, cron_expression, is_active)
VALUES (
    'trigger-1',
    'workflow-sample-1',
    'cron',
    '0 8 * * *',
    TRUE
)
ON CONFLICT (trigger_id) DO NOTHING;

-- ============================================
-- Success Message
-- ============================================
DO $$
BEGIN
    RAISE NOTICE '✅ Database schema initialized successfully!';
    RAISE NOTICE '📊 Tables created: workflows, execution_history, users, credentials, triggers';
    RAISE NOTICE '🔐 Sample user created: test@workflow.com';
    RAISE NOTICE '📝 Sample workflow created: Daily Email Summary';
END $$;
