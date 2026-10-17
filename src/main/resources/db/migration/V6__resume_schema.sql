-- V6: Resume schema
-- Creates the resumes table for storing user resumes and their versions.

CREATE TABLE resumes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    title VARCHAR(255) NOT NULL,
    content TEXT,
    is_base BOOLEAN NOT NULL,
    base_resume_id BIGINT REFERENCES resumes(id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_resumes_user_id ON resumes(user_id);
CREATE INDEX idx_resumes_base_resume_id ON resumes(base_resume_id);