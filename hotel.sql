CREATE DATABASE IF NOT EXISTS hotel CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE hotel;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
    phone VARCHAR(20) COMMENT '联系电话',
    role VARCHAR(20) NOT NULL COMMENT '角色(ADMIN/RECEPTIONIST/CLIENT)',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态(1启用/0禁用)',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    INDEX idx_username (username),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

CREATE TABLE IF NOT EXISTS room_type (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '类型ID',
    type_name VARCHAR(50) NOT NULL COMMENT '类型名称',
    price DECIMAL(10,2) NOT NULL COMMENT '单价/晚',
    capacity INT NOT NULL COMMENT '可住人数',
    bed_type VARCHAR(50) NOT NULL COMMENT '床型',
    area DECIMAL(6,2) COMMENT '面积(平方米)',
    description TEXT COMMENT '描述',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态(1可用/0不可用)',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    INDEX idx_type_name (type_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客房类型表';

CREATE TABLE IF NOT EXISTS room (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '客房ID',
    room_number VARCHAR(20) NOT NULL UNIQUE COMMENT '房间号',
    room_type_id BIGINT NOT NULL COMMENT '客房类型ID',
    floor INT NOT NULL COMMENT '楼层',
    status VARCHAR(20) NOT NULL COMMENT '状态(AVAILABLE/OCCUPIED/MAINTENANCE)',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    INDEX idx_room_number (room_number),
    INDEX idx_status (status),
    CONSTRAINT fk_room_room_type FOREIGN KEY (room_type_id) REFERENCES room_type(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客房表';

CREATE TABLE IF NOT EXISTS customer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '客户ID',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    id_card VARCHAR(20) NOT NULL UNIQUE COMMENT '身份证号',
    phone VARCHAR(20) NOT NULL COMMENT '联系电话',
    gender VARCHAR(10) NOT NULL COMMENT '性别',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    INDEX idx_id_card (id_card),
    INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户表';

CREATE TABLE IF NOT EXISTS `order` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订单ID',
    order_number VARCHAR(50) NOT NULL UNIQUE COMMENT '订单号',
    customer_id BIGINT NOT NULL COMMENT '客户ID',
    room_id BIGINT NOT NULL COMMENT '客房ID',
    check_in_date DATE NOT NULL COMMENT '入住日期',
    check_out_date DATE NOT NULL COMMENT '退房日期',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '总金额',
    status VARCHAR(20) NOT NULL COMMENT '状态(UNPAID/PAID/CANCELLED/COMPLETED)',
    remark TEXT COMMENT '备注',
    create_user_id BIGINT NOT NULL COMMENT '创建人ID',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    INDEX idx_order_number (order_number),
    INDEX idx_status (status),
    INDEX idx_customer_id (customer_id),
    INDEX idx_room_id (room_id),
    CONSTRAINT fk_order_customer FOREIGN KEY (customer_id) REFERENCES customer(id),
    CONSTRAINT fk_order_room FOREIGN KEY (room_id) REFERENCES room(id),
    CONSTRAINT fk_order_user FOREIGN KEY (create_user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

INSERT INTO sys_user (username, password, real_name, phone, role, status, create_time, update_time)
VALUES ('admin', '$2a$10$Q.Nv2A7fZQVA8Ff1VQ7j/u0BfWXc4wEeJjK5Dg3Q8TzN6NQW8M22K', '系统管理员', '13800138000', 'ADMIN', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE update_time = NOW();

INSERT INTO room_type (type_name, price, capacity, bed_type, area, description, status, create_time, update_time)
SELECT * FROM (
    SELECT '标准间', 280.00, 2, '双床', 25.00, '舒适的标准间，适合商务和休闲旅客', 1, NOW(), NOW()
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM room_type WHERE type_name = '标准间');

INSERT INTO room_type (type_name, price, capacity, bed_type, area, description, status, create_time, update_time)
SELECT * FROM (
    SELECT '大床房', 320.00, 2, '大床', 30.00, '宽敞的大床房，提供更舒适的睡眠体验', 1, NOW(), NOW()
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM room_type WHERE type_name = '大床房');

INSERT INTO room_type (type_name, price, capacity, bed_type, area, description, status, create_time, update_time)
SELECT * FROM (
    SELECT '豪华套房', 580.00, 3, '大床', 50.00, '豪华套房，带独立客厅，适合高端旅客', 1, NOW(), NOW()
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM room_type WHERE type_name = '豪华套房');

INSERT INTO room (room_number, room_type_id, floor, status, create_time, update_time)
SELECT '101', id, 1, 'AVAILABLE', NOW(), NOW() FROM room_type WHERE type_name = '标准间'
AND NOT EXISTS (SELECT 1 FROM room WHERE room_number = '101');

INSERT INTO room (room_number, room_type_id, floor, status, create_time, update_time)
SELECT '102', id, 1, 'AVAILABLE', NOW(), NOW() FROM room_type WHERE type_name = '标准间'
AND NOT EXISTS (SELECT 1 FROM room WHERE room_number = '102');

INSERT INTO room (room_number, room_type_id, floor, status, create_time, update_time)
SELECT '201', id, 2, 'AVAILABLE', NOW(), NOW() FROM room_type WHERE type_name = '大床房'
AND NOT EXISTS (SELECT 1 FROM room WHERE room_number = '201');

INSERT INTO room (room_number, room_type_id, floor, status, create_time, update_time)
SELECT '202', id, 2, 'AVAILABLE', NOW(), NOW() FROM room_type WHERE type_name = '大床房'
AND NOT EXISTS (SELECT 1 FROM room WHERE room_number = '202');

INSERT INTO room (room_number, room_type_id, floor, status, create_time, update_time)
SELECT '301', id, 3, 'AVAILABLE', NOW(), NOW() FROM room_type WHERE type_name = '豪华套房'
AND NOT EXISTS (SELECT 1 FROM room WHERE room_number = '301');