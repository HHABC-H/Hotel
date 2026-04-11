-- 酒店系统初始化数据（统一 user 表，密码使用 BCrypt）
-- 执行前请确认已在 hotel 库，且表结构为：user / room_type / room / order

USE hotel;

SET NAMES utf8mb4;

-- =========================
-- 1) 用户初始化（统一 user 表）
-- =========================
-- 明文密码请查看同目录文件：用户明文密码.txt
-- 注意：当前 SQL 中 password 字段均为 BCrypt 哈希

INSERT INTO `user` (`username`, `password`, `real_name`, `phone`, `id_card`, `gender`, `role`, `status`)
VALUES ('admin', '$2b$10$zmEipItn/wP8dI5NoUjvG.ccC.ftzzHZ5T8.oBBdKVqbWGfgER8ya', '系统管理员', '13800138000', '110101199001010001', 'UNKNOWN', 'ADMIN', 1)
ON DUPLICATE KEY UPDATE
`password` = VALUES(`password`),
`real_name` = VALUES(`real_name`),
`phone` = VALUES(`phone`),
`id_card` = VALUES(`id_card`),
`gender` = VALUES(`gender`),
`role` = VALUES(`role`),
`status` = VALUES(`status`);

INSERT INTO `user` (`username`, `password`, `real_name`, `phone`, `id_card`, `gender`, `role`, `status`)
VALUES ('front01', '$2b$10$gV6XWlTskk5EU3NdPjGQDe9fYFr.95mq3JShGw8lr9LhZybUwlIj6', '前台张', '13800138001', '110101199202020002', 'F', 'RECEPTIONIST', 1)
ON DUPLICATE KEY UPDATE
`password` = VALUES(`password`),
`real_name` = VALUES(`real_name`),
`phone` = VALUES(`phone`),
`id_card` = VALUES(`id_card`),
`gender` = VALUES(`gender`),
`role` = VALUES(`role`),
`status` = VALUES(`status`);

INSERT INTO `user` (`username`, `password`, `real_name`, `phone`, `id_card`, `gender`, `role`, `status`)
VALUES ('client01', '$2b$10$K5z5WTv0mtkFp4/tMKLfj.N3UACGsapTg6YmaEfIS5Jk3A5M8pLDi', '客户李', '13800138002', '110101199503030003', 'M', 'CLIENT', 1)
ON DUPLICATE KEY UPDATE
`password` = VALUES(`password`),
`real_name` = VALUES(`real_name`),
`phone` = VALUES(`phone`),
`id_card` = VALUES(`id_card`),
`gender` = VALUES(`gender`),
`role` = VALUES(`role`),
`status` = VALUES(`status`);

INSERT INTO `user` (`username`, `password`, `real_name`, `phone`, `id_card`, `gender`, `role`, `status`)
VALUES ('client02', '$2b$10$WbDYmTplT5KvAJB/I.ON6eVMihLM1eXR39yExYZzSw30pe7eJGsZe', '客户王', '13800138003', '110101199607040004', 'F', 'CLIENT', 1)
ON DUPLICATE KEY UPDATE
`password` = VALUES(`password`),
`real_name` = VALUES(`real_name`),
`phone` = VALUES(`phone`),
`id_card` = VALUES(`id_card`),
`gender` = VALUES(`gender`),
`role` = VALUES(`role`),
`status` = VALUES(`status`);

-- =========================
-- 2) 客房类型初始化
-- =========================
INSERT INTO room_type (type_name, price, capacity, bed_type, area, description, status)
SELECT * FROM (
    SELECT '标准间', 280.00, 2, '双床', 25.00, '舒适标准间', 1
) AS t
WHERE NOT EXISTS (SELECT 1 FROM room_type WHERE type_name = '标准间');

INSERT INTO room_type (type_name, price, capacity, bed_type, area, description, status)
SELECT * FROM (
    SELECT '大床房', 320.00, 2, '大床', 30.00, '舒适大床房', 1
) AS t
WHERE NOT EXISTS (SELECT 1 FROM room_type WHERE type_name = '大床房');

INSERT INTO room_type (type_name, price, capacity, bed_type, area, description, status)
SELECT * FROM (
    SELECT '豪华套房', 580.00, 3, '大床', 50.00, '豪华套房，含会客区', 1
) AS t
WHERE NOT EXISTS (SELECT 1 FROM room_type WHERE type_name = '豪华套房');

-- =========================
-- 3) 客房初始化
-- =========================
INSERT INTO room (room_number, room_type_id, floor, status)
SELECT '101', id, 1, 'AVAILABLE' FROM room_type WHERE type_name = '标准间'
AND NOT EXISTS (SELECT 1 FROM room WHERE room_number = '101');

INSERT INTO room (room_number, room_type_id, floor, status)
SELECT '102', id, 1, 'AVAILABLE' FROM room_type WHERE type_name = '标准间'
AND NOT EXISTS (SELECT 1 FROM room WHERE room_number = '102');

INSERT INTO room (room_number, room_type_id, floor, status)
SELECT '201', id, 2, 'AVAILABLE' FROM room_type WHERE type_name = '大床房'
AND NOT EXISTS (SELECT 1 FROM room WHERE room_number = '201');

INSERT INTO room (room_number, room_type_id, floor, status)
SELECT '202', id, 2, 'AVAILABLE' FROM room_type WHERE type_name = '大床房'
AND NOT EXISTS (SELECT 1 FROM room WHERE room_number = '202');

INSERT INTO room (room_number, room_type_id, floor, status)
SELECT '301', id, 3, 'AVAILABLE' FROM room_type WHERE type_name = '豪华套房'
AND NOT EXISTS (SELECT 1 FROM room WHERE room_number = '301');

