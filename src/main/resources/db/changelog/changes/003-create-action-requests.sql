-- liquibase formatted sql
-- changeset madan:3
CREATE TABLE action_requests (
                                 id UUID NOT NULL,
                                 action_type VARCHAR(255) NOT NULL,
                                 decision_reason VARCHAR(2000),
                                 executed_at TIMESTAMP(6),
                                 execution_error VARCHAR(4000),
                                 execution_result VARCHAR(4000),
                                 payload VARCHAR(4000) NOT NULL,
                                 requested_at TIMESTAMP(6) NOT NULL,
                                 requested_by VARCHAR(255) NOT NULL,
                                 reviewed_at TIMESTAMP(6),
                                 reviewed_by VARCHAR(255),
                                 status VARCHAR(255) NOT NULL,
                                 PRIMARY KEY (id)
);