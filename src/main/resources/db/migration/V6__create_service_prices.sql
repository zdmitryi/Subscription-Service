CREATE TABLE IF NOT EXISTS service_prices (
    id SERIAL PRIMARY KEY,
    service_name VARCHAR(255) NOT NULL UNIQUE,
    monthly_price DECIMAL(10,2) NOT NULL
);

INSERT INTO service_prices (service_name, monthly_price) VALUES
('Service 1', 1000),
('Service 2', 1500),
('Service 3', 2000);