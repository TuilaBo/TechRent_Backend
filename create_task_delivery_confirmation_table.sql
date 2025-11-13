CREATE TABLE IF NOT EXISTS task_delivery_confirmation (
    confirmation_id SERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES task(task_id) ON DELETE CASCADE,
    staff_id BIGINT NOT NULL REFERENCES staff(staff_id) ON DELETE CASCADE,
    confirmed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_task_staff UNIQUE (task_id, staff_id)
);
