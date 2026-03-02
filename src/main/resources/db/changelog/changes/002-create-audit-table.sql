-- liquibase formatted sql
-- changeset madan:2
CREATE TABLE agent_audit_logs (
                                  id UUID NOT NULL,
                                  agent_name VARCHAR(255),
                                  ai_response VARCHAR(5000),
                                  chat_id VARCHAR(255),
                                  execution_time_ms BIGINT NOT NULL,
                                  timestamp TIMESTAMP(6),
                                  total_tokens INTEGER NOT NULL,
                                  user_query VARCHAR(5000),
                                  PRIMARY KEY (id)
);