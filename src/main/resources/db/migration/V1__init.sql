CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS tb_user (
    id       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email    VARCHAR(200) NOT NULL UNIQUE,
    password VARCHAR(129) NOT NULL,
    name     VARCHAR(120),
    role     VARCHAR(20)  NOT NULL DEFAULT 'ROLE_USER',
    version  BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS tb_user_external_project (
    user_id     UUID         NOT NULL,
    project_id  UUID         NOT NULL,
    name        VARCHAR(120) NOT NULL,
    description VARCHAR(255),
    version     BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_user_external_project PRIMARY KEY (user_id, project_id),
    CONSTRAINT fk_project_user FOREIGN KEY (user_id)
        REFERENCES tb_user(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_email   ON tb_user(email);
CREATE INDEX IF NOT EXISTS idx_project_user ON tb_user_external_project(user_id);

