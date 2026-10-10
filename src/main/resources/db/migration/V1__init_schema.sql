CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100)  NOT NULL,
    email         VARCHAR(150)  NOT NULL,
    password_hash VARCHAR(100)  NOT NULL,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE projects (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(120)  NOT NULL,
    description VARCHAR(2000),
    deadline    DATE,
    status      VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    owner_id    BIGINT        NOT NULL REFERENCES users (id),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_projects_owner ON projects (owner_id);

CREATE TABLE project_members (
    id             BIGSERIAL PRIMARY KEY,
    project_id     BIGINT       NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    user_id        BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role           VARCHAR(20)  NOT NULL DEFAULT 'MEMBER',
    responsibility VARCHAR(150),
    joined_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_project_member UNIQUE (project_id, user_id)
);
CREATE INDEX idx_members_user ON project_members (user_id);

CREATE TABLE tasks (
    id           BIGSERIAL PRIMARY KEY,
    project_id   BIGINT        NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    title        VARCHAR(160)  NOT NULL,
    description  VARCHAR(2000),
    status       VARCHAR(20)   NOT NULL DEFAULT 'TODO',
    priority     VARCHAR(20)   NOT NULL DEFAULT 'MEDIUM',
    due_date     DATE,
    assignee_id  BIGINT        REFERENCES users (id) ON DELETE SET NULL,
    created_by   BIGINT        NOT NULL REFERENCES users (id),
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);
CREATE INDEX idx_tasks_project  ON tasks (project_id);
CREATE INDEX idx_tasks_assignee ON tasks (assignee_id);

CREATE TABLE activities (
    id         BIGSERIAL PRIMARY KEY,
    project_id BIGINT        NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    user_id    BIGINT        REFERENCES users (id) ON DELETE SET NULL,
    message    VARCHAR(300)  NOT NULL,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_activities_project ON activities (project_id, created_at DESC);
