-- V2: Jobs ingestion tables

CREATE TABLE jobs (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(512) NOT NULL,
    company         VARCHAR(255) NOT NULL,
    location        VARCHAR(255) NOT NULL,
    remote_status   VARCHAR(20),
    employment_type VARCHAR(50),
    description     TEXT,
    application_url VARCHAR(1024),
    source          VARCHAR(100) NOT NULL,
    source_job_id   VARCHAR(255) NOT NULL,
    posted_date     DATE,
    deadline        DATE,
    ingested_at     TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX idx_jobs_source_jobid ON jobs(source, source_job_id);
CREATE INDEX idx_jobs_company ON jobs(company);

CREATE TABLE job_skills (
    id          BIGSERIAL PRIMARY KEY,
    job_id      BIGINT NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    skill_name  VARCHAR(255) NOT NULL
);
CREATE INDEX idx_job_skills_job ON job_skills(job_id);