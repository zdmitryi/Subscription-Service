CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    subscription_id BIGINT NOT NULL,
    scheduled_at TIMESTAMP NOT NULL,
    is_sent BOOLEAN DEFAULT FALSE
);