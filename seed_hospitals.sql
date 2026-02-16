-- Hospital Seed Data for Emergency 108
-- Run this script in your PostgreSQL database to add 5 major hospitals in New Delhi

-- First, check if hospitals table exists
-- If not, create it (JPA should have created it, but just in case)
CREATE TABLE IF NOT EXISTS hospitals (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    address VARCHAR(500),
    phone VARCHAR(20),
    is_active BOOLEAN DEFAULT true
);

-- Clear existing hospitals (optional - remove this line if you want to keep existing data)
-- DELETE FROM hospitals;

-- Insert 5 major hospitals in New Delhi area
INSERT INTO hospitals (name, latitude, longitude, address, phone, is_active) VALUES
('AIIMS Delhi', 28.5672, 77.2100, 'Ansari Nagar, New Delhi - 110029', '011-26588500', true),
('Safdarjung Hospital', 28.5677, 77.2062, 'Ring Road, New Delhi - 110029', '011-26165060', true),
('Ram Manohar Lohia Hospital', 28.6263, 77.2088, 'Baba Kharak Singh Marg, New Delhi - 110001', '011-23404444', true),
('Sir Ganga Ram Hospital', 28.6425, 77.1952, 'Rajinder Nagar, New Delhi - 110060', '011-25750000', true),
('Max Super Speciality Hospital', 28.5428, 77.2713, 'Press Enclave Road, Saket, New Delhi - 110017', '011-26515050', true)
ON CONFLICT DO NOTHING;

-- Verify the data
SELECT id, name, latitude, longitude, is_active FROM hospitals;
