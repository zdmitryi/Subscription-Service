ALTER TABLE subscriptions
ALTER COLUMN service_name TYPE VARCHAR(255) USING service_name::VARCHAR;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'subscription_status') THEN
        CREATE TYPE subscription_status AS ENUM ('ACTIVE', 'SUSPENDED', 'CANCELLED', 'EXPIRED');
    END IF;
END$$;

ALTER TABLE subscriptions
ALTER COLUMN status TYPE subscription_status USING status::subscription_status;