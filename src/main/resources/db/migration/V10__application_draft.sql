CREATE TABLE IF NOT EXISTS application_drafts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    job_id BIGINT REFERENCES jobs(id) ON DELETE SET NULL,
    resume_id BIGINT REFERENCES resumes(id) ON DELETE SET NULL,
    destination_url VARCHAR(500),
    proposed_fields TEXT,
    unresolved_fields VARCHAR(500),
    approved BOOLEAN DEFAULT FALSE,
    approval_timestamp TIMESTAMP,
    status VARCHAR(50) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
