-- YesChill AI 应用生成平台 数据库初始化脚本
-- 数据库: yeschill_ai_code

CREATE DATABASE IF NOT EXISTS `yeschill_ai_code` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `yeschill_ai_code`;

-- ----------------------------
-- 用户表
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`          BIGINT        NOT NULL COMMENT '主键，雪花ID',
    `userAccount` VARCHAR(256)  NOT NULL COMMENT '账号',
    `userPassword`VARCHAR(512)  NOT NULL COMMENT '密码',
    `userName`    VARCHAR(256)  DEFAULT NULL COMMENT '用户昵称',
    `userAvatar`  VARCHAR(1024) DEFAULT NULL COMMENT '用户头像',
    `userProfile` VARCHAR(512)  DEFAULT NULL COMMENT '用户简介',
    `userRole`    VARCHAR(256)  NOT NULL DEFAULT 'user' COMMENT '用户角色：user/admin',
    `editTime`    DATETIME      DEFAULT NULL COMMENT '编辑时间',
    `createTime`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`    TINYINT       NOT NULL DEFAULT 0 COMMENT '是否删除(0-未删,1-已删)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_userAccount` (`userAccount`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ----------------------------
-- 应用表
-- ----------------------------
DROP TABLE IF EXISTS `app`;
CREATE TABLE `app` (
    `id`           BIGINT        NOT NULL COMMENT '主键，雪花ID',
    `appName`      VARCHAR(256)  DEFAULT NULL COMMENT '应用名称',
    `cover`        VARCHAR(1024) DEFAULT NULL COMMENT '应用封面',
    `initPrompt`   TEXT          DEFAULT NULL COMMENT '应用初始化的prompt',
    `codeGenType`  VARCHAR(128)  DEFAULT NULL COMMENT '代码生成类型：html/multi_file/vue_project',
    `genMode`      VARCHAR(32)   NOT NULL DEFAULT 'normal' COMMENT '生成模式：normal-快速模式，workflow-工作流模式',
    `deployKey`    VARCHAR(64)   DEFAULT NULL COMMENT '部署标识（6位随机字符串）',
    `deployedTime` DATETIME      DEFAULT NULL COMMENT '部署时间',
    `deployStatus` VARCHAR(32)   DEFAULT NULL COMMENT '部署状态：QUEUED/BUILDING/DONE/FAILED',
    `priority`     INT           NOT NULL DEFAULT 0 COMMENT '优先级（99=精选）',
    `userId`       BIGINT        NOT NULL COMMENT '创建用户ID',
    `editTime`     DATETIME      DEFAULT NULL COMMENT '编辑时间',
    `createTime`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`     TINYINT       NOT NULL DEFAULT 0 COMMENT '是否删除(0-未删,1-已删)',
    PRIMARY KEY (`id`),
    KEY `idx_userId` (`userId`),
    KEY `idx_deployStatus` (`deployStatus`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用表';

-- ----------------------------
-- 对话历史表
-- ----------------------------
DROP TABLE IF EXISTS `chat_history`;
CREATE TABLE `chat_history` (
    `id`          BIGINT       NOT NULL COMMENT '主键，雪花ID',
    `message`     LONGTEXT     DEFAULT NULL COMMENT '消息内容',
    `messageType` VARCHAR(32)  DEFAULT NULL COMMENT '消息类型：user/ai',
    `appId`       BIGINT       NOT NULL COMMENT '应用ID',
    `userId`      BIGINT       NOT NULL COMMENT '用户ID',
    `createTime`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete`    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否删除(0-未删,1-已删)',
    PRIMARY KEY (`id`),
    KEY `idx_appId` (`appId`),
    KEY `idx_userId` (`userId`),
    KEY `idx_createTime` (`createTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话历史表';
