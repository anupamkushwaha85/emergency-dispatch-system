-- Insert 5 major hospitals in New Delhi area
INSERT INTO hospitals (name, latitude, longitude, address, phone, is_active) VALUES
('AIIMS Delhi', 28.5672, 77.2100, 'Ansari Nagar, New Delhi - 110029', '011-26588500', true),
('Safdarjung Hospital', 28.5677, 77.2062, 'Ring Road, New Delhi - 110029', '011-26165060', true),
('Ram Manohar Lohia Hospital', 28.6263, 77.2088, 'Baba Kharak Singh Marg, New Delhi - 110001', '011-23404444', true),
('Sir Ganga Ram Hospital', 28.6425, 77.1952, 'Rajinder Nagar, New Delhi - 110060', '011-25750000', true),
('Max Super Speciality Hospital', 28.5428, 77.2713, 'Press Enclave Road, Saket, New Delhi - 110017', '011-26515050', true);

-- Insert Default Ambulance (ID 1) specifically for dev testing
INSERT INTO ambulances (id, vehicle_number, type, is_active, latitude, longitude) VALUES
(1, 'DL-108-0001', 'ALS', true, 28.6139, 77.2090)
ON DUPLICATE KEY UPDATE is_active=true;
