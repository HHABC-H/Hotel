-- Hotel system seed data (single user table, BCrypt passwords)
-- Run this in database: hotel

USE hotel;

SET NAMES utf8mb4;

ALTER TABLE `user`
ADD COLUMN IF NOT EXISTS `balance` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'Account balance';

-- =========================
-- 1) Users
-- =========================

INSERT INTO `user` (`username`, `password`, `real_name`, `phone`, `id_card`, `gender`, `role`, `status`, `balance`)
VALUES ('admin', '$2b$10$zmEipItn/wP8dI5NoUjvG.ccC.ftzzHZ5T8.oBBdKVqbWGfgER8ya', 'System Admin', '13800138000', '110101199001010001', 'UNKNOWN', 'ADMIN', 1, 0.00)
ON DUPLICATE KEY UPDATE
`password` = VALUES(`password`),
`real_name` = VALUES(`real_name`),
`phone` = VALUES(`phone`),
`id_card` = VALUES(`id_card`),
`gender` = VALUES(`gender`),
`role` = VALUES(`role`),
`status` = VALUES(`status`),
`balance` = VALUES(`balance`);

INSERT INTO `user` (`username`, `password`, `real_name`, `phone`, `id_card`, `gender`, `role`, `status`, `balance`)
VALUES ('front01', '$2b$10$gV6XWlTskk5EU3NdPjGQDe9fYFr.95mq3JShGw8lr9LhZybUwlIj6', 'Reception A', '13800138001', '110101199202020002', 'F', 'RECEPTIONIST', 1, 0.00)
ON DUPLICATE KEY UPDATE
`password` = VALUES(`password`),
`real_name` = VALUES(`real_name`),
`phone` = VALUES(`phone`),
`id_card` = VALUES(`id_card`),
`gender` = VALUES(`gender`),
`role` = VALUES(`role`),
`status` = VALUES(`status`),
`balance` = VALUES(`balance`);

INSERT INTO `user` (`username`, `password`, `real_name`, `phone`, `id_card`, `gender`, `role`, `status`, `balance`)
VALUES ('client01', '$2b$10$K5z5WTv0mtkFp4/tMKLfj.N3UACGsapTg6YmaEfIS5Jk3A5M8pLDi', 'Client Zhang', '13800138002', '110101199503030003', 'M', 'CLIENT', 1, 5000.00)
ON DUPLICATE KEY UPDATE
`password` = VALUES(`password`),
`real_name` = VALUES(`real_name`),
`phone` = VALUES(`phone`),
`id_card` = VALUES(`id_card`),
`gender` = VALUES(`gender`),
`role` = VALUES(`role`),
`status` = VALUES(`status`),
`balance` = VALUES(`balance`);

INSERT INTO `user` (`username`, `password`, `real_name`, `phone`, `id_card`, `gender`, `role`, `status`, `balance`)
VALUES ('client02', '$2b$10$WbDYmTplT5KvAJB/I.ON6eVMihLM1eXR39yExYZzSw30pe7eJGsZe', 'Client Wang', '13800138003', '110101199607040004', 'F', 'CLIENT', 1, 5000.00)
ON DUPLICATE KEY UPDATE
`password` = VALUES(`password`),
`real_name` = VALUES(`real_name`),
`phone` = VALUES(`phone`),
`id_card` = VALUES(`id_card`),
`gender` = VALUES(`gender`),
`role` = VALUES(`role`),
`status` = VALUES(`status`),
`balance` = VALUES(`balance`);

-- =========================
-- 2) Room types
-- =========================

INSERT INTO room_type (type_name, price, capacity, bed_type, area, description, status)
SELECT * FROM (
    SELECT 'Standard Room', 280.00, 2, 'Twin', 25.00, 'Comfortable standard room', 1
) AS t
WHERE NOT EXISTS (SELECT 1 FROM room_type WHERE type_name = 'Standard Room');

INSERT INTO room_type (type_name, price, capacity, bed_type, area, description, status)
SELECT * FROM (
    SELECT 'King Room', 320.00, 2, 'King', 30.00, 'Comfortable king room', 1
) AS t
WHERE NOT EXISTS (SELECT 1 FROM room_type WHERE type_name = 'King Room');

INSERT INTO room_type (type_name, price, capacity, bed_type, area, description, status)
SELECT * FROM (
    SELECT 'Deluxe Suite', 580.00, 3, 'King', 50.00, 'Deluxe suite with lounge area', 1
) AS t
WHERE NOT EXISTS (SELECT 1 FROM room_type WHERE type_name = 'Deluxe Suite');

-- =========================
-- 3) Rooms
-- =========================

INSERT INTO room (room_number, room_type_id, floor, status)
SELECT '101', id, 1, 'AVAILABLE' FROM room_type WHERE type_name = 'Standard Room'
AND NOT EXISTS (SELECT 1 FROM room WHERE room_number = '101');

INSERT INTO room (room_number, room_type_id, floor, status)
SELECT '102', id, 1, 'AVAILABLE' FROM room_type WHERE type_name = 'Standard Room'
AND NOT EXISTS (SELECT 1 FROM room WHERE room_number = '102');

INSERT INTO room (room_number, room_type_id, floor, status)
SELECT '201', id, 2, 'AVAILABLE' FROM room_type WHERE type_name = 'King Room'
AND NOT EXISTS (SELECT 1 FROM room WHERE room_number = '201');

INSERT INTO room (room_number, room_type_id, floor, status)
SELECT '202', id, 2, 'AVAILABLE' FROM room_type WHERE type_name = 'King Room'
AND NOT EXISTS (SELECT 1 FROM room WHERE room_number = '202');

INSERT INTO room (room_number, room_type_id, floor, status)
SELECT '301', id, 3, 'AVAILABLE' FROM room_type WHERE type_name = 'Deluxe Suite'
AND NOT EXISTS (SELECT 1 FROM room WHERE room_number = '301');
