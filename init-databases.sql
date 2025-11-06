-- Initialize databases for all system designs

-- Parking Lot Database
CREATE DATABASE IF NOT EXISTS parkinglot_db;

-- Dropbox Database  
CREATE DATABASE IF NOT EXISTS dropbox_db;

-- Payment Service Database
CREATE DATABASE IF NOT EXISTS payment_db;

-- Job Scheduler Database
CREATE DATABASE IF NOT EXISTS jobscheduler_db;

-- Digital Payment Database
CREATE DATABASE IF NOT EXISTS digitalpayment_db;

-- Ticket Booking Database
CREATE DATABASE IF NOT EXISTS ticketbooking_db;

-- Instagram Database
CREATE DATABASE IF NOT EXISTS instagram_db;

-- Rate Limiter Database
CREATE DATABASE IF NOT EXISTS ratelimiter_db;

-- Grant permissions (adjust username as needed)
GRANT ALL PRIVILEGES ON parkinglot_db.* TO 'postgres'@'%';
GRANT ALL PRIVILEGES ON dropbox_db.* TO 'postgres'@'%';
GRANT ALL PRIVILEGES ON payment_db.* TO 'postgres'@'%';
GRANT ALL PRIVILEGES ON jobscheduler_db.* TO 'postgres'@'%';
GRANT ALL PRIVILEGES ON digitalpayment_db.* TO 'postgres'@'%';
GRANT ALL PRIVILEGES ON ticketbooking_db.* TO 'postgres'@'%';
GRANT ALL PRIVILEGES ON instagram_db.* TO 'postgres'@'%';
GRANT ALL PRIVILEGES ON ratelimiter_db.* TO 'postgres'@'%';

FLUSH PRIVILEGES;