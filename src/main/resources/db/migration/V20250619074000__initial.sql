CREATE SEQUENCE jobs_id_seq;

CREATE TABLE jobs (
    id int NOT NULL PRIMARY KEY DEFAULT nextval('jobs_id_seq'),
    job_type TEXT NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    status text NOT NULL CHECK (status IN ('Pending', 'InProgress', 'Completed', 'Failed')),
    start_after TIMESTAMP NOT NULL DEFAULT NOW(),
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    expires_at TIMESTAMP,
    attempt int NOT NULL DEFAULT 0,
    last_error TEXT,
    worker_id TEXT
)