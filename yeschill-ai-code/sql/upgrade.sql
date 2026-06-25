-- ============================================================
-- YesChill AI Code 数据库升级脚本
-- 适用于已有数据库的增量升级，所有 DDL 均为幂等（字段已存在则跳过）
-- 执行方式: mysql -u root -p yeschill_ai_code < upgrade.sql
-- ============================================================

-- 1. 应用表增加 deployStatus 字段（部署状态）
--    如果字段已存在会报错，忽略即可
ALTER TABLE `app` ADD COLUMN `deployStatus` VARCHAR(32) DEFAULT NULL COMMENT '部署状态：QUEUED/BUILDING/DONE/FAILED';

-- 2. 应用表增加 genMode 字段（生成模式）
--    normal-快速模式(默认)，workflow-工作流模式
ALTER TABLE `app` ADD COLUMN `genMode` VARCHAR(32) NOT NULL DEFAULT 'normal' COMMENT '生成模式：normal-快速模式，workflow-工作流模式';

-- 3. 部署状态索引（提升队列消费查询性能）
--    MySQL 8.0+ 支持 IF NOT EXISTS 语法
ALTER TABLE `app` ADD INDEX IF NOT EXISTS `idx_deployStatus` (`deployStatus`);
