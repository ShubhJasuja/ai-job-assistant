-- V7: Job active-state and update tracking
-- Allows deactivation/archival of jobs without deleting data.

ALTER TABLE jobs ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE jobs ADD COLUMN updated_at TIMESTAMP;

CREATE INDEX idx_jobs_is_active ON jobs(is_active);
CREATE INDEX idx_jobs_company_active ON jobs(company, is_active);
