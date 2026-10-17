-- V3: Job matches table

CREATE TABLE job_matches (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT NOT NULL REFERENCES users(id),
    job_id            BIGINT NOT NULL REFERENCES jobs(id),
    score             INT NOT NULL,
    explanation       TEXT,
    matched_skills    TEXT,
    missing_skills    TEXT,
    calculated_at     TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX idx_job_matches_user_job ON job_matches(user_id, job_id);
CREATE INDEX idx_job_matches_user_score ON job_matches(user_id, score DESC);