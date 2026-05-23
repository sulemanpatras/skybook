-- ==========================================
-- SkyBook Database Setup Script
-- Run this ONCE before starting the backend
-- ==========================================

-- Create database and user
CREATE USER skybook_user WITH PASSWORD 'skybook_pass';
CREATE DATABASE skybook_db OWNER skybook_user;
GRANT ALL PRIVILEGES ON DATABASE skybook_db TO skybook_user;

-- Connect to skybook_db then run:
\c skybook_db;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'PASSENGER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Flights table
CREATE TABLE IF NOT EXISTS flights (
    id VARCHAR(20) PRIMARY KEY,
    airline VARCHAR(100) NOT NULL,
    source VARCHAR(100) NOT NULL,
    destination VARCHAR(100) NOT NULL,
    departure_time TIMESTAMP NOT NULL,
    arrival_time TIMESTAMP NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    seats_available INT NOT NULL,
    total_seats INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tickets table
CREATE TABLE IF NOT EXISTS tickets (
    id VARCHAR(20) PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    flight_id VARCHAR(20) REFERENCES flights(id),
    passenger_name VARCHAR(100) NOT NULL,
    seat_number VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    booked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    google_event_id VARCHAR(255),
    calendar_event_url VARCHAR(500)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_tickets_user_id ON tickets(user_id);
CREATE INDEX IF NOT EXISTS idx_tickets_flight_id ON tickets(flight_id);
CREATE INDEX IF NOT EXISTS idx_flights_source_dest ON flights(source, destination);

-- ==========================================
-- SEED DATA — Sample Flights
-- ==========================================
INSERT INTO flights (id, airline, source, destination, departure_time, arrival_time, price, seats_available, total_seats) VALUES
('FL001', 'PIA', 'Karachi', 'Lahore',      '2025-07-15 08:00:00', '2025-07-15 10:00:00', 120.00, 50, 50),
('FL002', 'AirBlue', 'Lahore', 'Islamabad', '2025-07-16 14:30:00', '2025-07-16 15:30:00',  85.00, 30, 30),
('FL003', 'Emirates', 'Karachi', 'Dubai',   '2025-07-17 22:00:00', '2025-07-18 00:30:00', 350.00, 100, 100),
('FL004', 'Qatar',   'Islamabad', 'London', '2025-07-18 03:00:00', '2025-07-18 09:00:00', 750.00, 40, 40),
('FL005', 'Turkish', 'Lahore', 'Istanbul',  '2025-07-20 11:00:00', '2025-07-20 16:00:00', 480.00, 60, 60)
ON CONFLICT DO NOTHING;

-- Default admin user (password: Admin@123 — bcrypt hashed)
INSERT INTO users (full_name, email, password, role) VALUES
('Admin', 'admin@skybook.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN')
ON CONFLICT DO NOTHING;

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO skybook_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO skybook_user;
