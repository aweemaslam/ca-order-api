-- liquibase formatted sql

-- changeset aweem:ca-shedlock splitStatements:false runOnChange:false
-- comment: Global distributed coordination task scheduler locking mechanism table.
-- rollback DROP TABLE shedlock;
CREATE TABLE shedlock
(
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP    NOT NULL,
    locked_at  TIMESTAMP    NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    CONSTRAINT pk_shedlock PRIMARY KEY (name)
);

CREATE INDEX idx_shedlock_lock_until ON shedlock (lock_until);

