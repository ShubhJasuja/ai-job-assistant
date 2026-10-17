-- V4: Application tracker tables

CREATE TABLE applications (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    job_id          BIGINT NOT NULL REFERENCES jobs(id),
    status          VARCHAR(20) NOT NULL,
    notes           TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP
);
CREATE INDEX idx_applications_user ON applications(user_id);
CREATE INDEX idx_applications_status ON applications(status);

CREATE TABLE application_events (
    id                BIGSERIAL PRIMARY KEY,
    application_id    BIGINT NOT NULL REFERENCES applications(id),
    from_status       VARCHAR(20),
    to_status         VARCHAR(20) NOT NULL,
    notes             TEXT,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_app_events_app ON application_events(application_id);
