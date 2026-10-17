-- V5: Candidate profile schema

CREATE TABLE candidate_profiles (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    headline        VARCHAR(255),
    summary         TEXT,
    preferred_roles  VARCHAR(255),
    preferred_locations VARCHAR(255),
    remote_preference BOOLEAN,
    experience_level INTEGER,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP
);

CREATE UNIQUE INDEX idx_candidate_profiles_user ON candidate_profiles(user_id);

CREATE TABLE educations (
    id              BIGSERIAL PRIMARY KEY,
    profile_id      BIGINT NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
    degree          VARCHAR(255),
    university      VARCHAR(255),
    field_of_study  VARCHAR(255),
    graduation_date DATE,
    gpa            DOUBLE PRECISION,
    description     TEXT
);

CREATE INDEX idx_educations_profile ON educations(profile_id);

CREATE TABLE experiences (
    id              BIGSERIAL PRIMARY KEY,
    profile_id      BIGINT NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
    company         VARCHAR(255) NOT NULL,
    role           VARCHAR(255) NOT NULL,
    description    TEXT,
    start_date     DATE NOT NULL,
    end_date       DATE
);

CREATE INDEX idx_experiences_profile ON experiences(profile_id);

CREATE TABLE projects (
    id              BIGSERIAL PRIMARY KEY,
    profile_id      BIGINT NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
    title          VARCHAR(255) NOT NULL,
    description    TEXT,
    technologies   TEXT,
    url            VARCHAR(255)
);

CREATE INDEX idx_projects_profile ON projects(profile_id);

CREATE TABLE skills (
    id              BIGSERIAL PRIMARY KEY,
    profile_id      BIGINT NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
    name           VARCHAR(255) NOT NULL
);

CREATE INDEX idx_skills_profile ON skills(profile_id);

CREATE TABLE preferences (
    id              BIGSERIAL PRIMARY KEY,
    profile_id      BIGINT NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
    role           VARCHAR(255),
    location       VARCHAR(255),
    remote_ok      BOOLEAN,
    experience_level VARCHAR(50)
);

CREATE UNIQUE INDEX idx_preferences_profile ON preferences(profile_id);