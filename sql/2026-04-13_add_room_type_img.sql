-- 为 room_type 增加房型图片字段（OSS URL）
ALTER TABLE `room_type`
ADD COLUMN `img` VARCHAR(500) NULL COMMENT '房型图片URL' AFTER `description`;

