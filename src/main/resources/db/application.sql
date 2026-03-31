-- 低代码平台数据库表结构
-- 字符集: utf8mb4

-- 1. 应用表
CREATE TABLE IF NOT EXISTS `application` (
    `id`            BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id`       BIGINT NOT NULL COMMENT '所属用户ID',
    `name`          VARCHAR(100) NOT NULL COMMENT '应用名称',
    `description`   TEXT COMMENT '应用描述',
    `status`        ENUM('ACTIVE','ARCHIVED') NOT NULL DEFAULT 'ACTIVE',
    `thumbnail_url` VARCHAR(500) COMMENT '预览缩略图地址',
    `latest_version_id` BIGINT DEFAULT NULL COMMENT '最新版本ID',
    `created_at`    BIGINT(20) NOT NULL DEFAULT 0 COMMENT '创建时间戳（毫秒）',
    `updated_at`    BIGINT(20) NOT NULL DEFAULT 0 COMMENT '更新时间戳（毫秒）',
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='低代码应用表';

-- 2. 生成任务表
CREATE TABLE IF NOT EXISTS `generation_task` (
    `id`                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    `app_id`              BIGINT NOT NULL COMMENT '所属应用ID',
    `user_id`             BIGINT NOT NULL COMMENT '所属用户ID',
    `prompt`              TEXT NOT NULL COMMENT '用户输入提示词',
    `api_doc_text`        TEXT COMMENT '用户提供的API文档文本（可为空）',
    `parent_version_id`   BIGINT DEFAULT NULL COMMENT '基于哪个版本迭代（首次为NULL）',
    `status`              ENUM('PENDING','RUNNING','SUCCESS','FAILED') NOT NULL DEFAULT 'PENDING',
    `retry_count`         INT NOT NULL DEFAULT 0 COMMENT '已重试次数',
    `max_retry`           INT NOT NULL DEFAULT 3 COMMENT '最大重试次数',
    `workflow_thread_id`  VARCHAR(100) COMMENT 'LangGraph4j threadId',
    `error_message`       TEXT COMMENT '失败原因',
    `created_at`          BIGINT(20) NOT NULL DEFAULT 0 COMMENT '创建时间戳（毫秒）',
    `finished_at`         BIGINT(20) DEFAULT NULL COMMENT '完成时间戳（毫秒）',
    INDEX `idx_app_id` (`app_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码生成任务表';

-- 3. 生成版本表（每个应用对应一条记录，只保留最新代码）
CREATE TABLE IF NOT EXISTS `generated_version` (
    `id`                BIGINT AUTO_INCREMENT PRIMARY KEY,
    `app_id`            BIGINT NOT NULL COMMENT '所属应用ID',
    `task_id`           BIGINT NOT NULL COMMENT '最后一次生成此应用代码的任务ID',
    `code_storage_path` VARCHAR(500) COMMENT '代码文件存储路径',
    `preview_url`       VARCHAR(500) COMMENT '预览/部署地址',
    `download_url`      VARCHAR(500) COMMENT '下载地址',
    `file_size`         BIGINT COMMENT '代码包大小（字节）',
    `validation_result` JSON COMMENT '代码校验详情',
    `prompt_snapshot`   TEXT COMMENT '生成时的完整提示词快照',
    `project_summary`   TEXT COMMENT '项目文件摘要（大模型返回，简要描述每个文件作用）',
    `created_at`        BIGINT(20) NOT NULL DEFAULT 0 COMMENT '创建时间戳（毫秒）',
    `updated_at`        BIGINT(20) NOT NULL DEFAULT 0 COMMENT '更新时间戳（毫秒）',
    UNIQUE KEY `uk_app_id` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生成代码表（每个应用一条记录，只保留最新）';

-- 4. 对话消息表
CREATE TABLE IF NOT EXISTS `conversation_message` (
    `id`          BIGINT AUTO_INCREMENT PRIMARY KEY,
    `app_id`      BIGINT NOT NULL COMMENT '所属应用ID',
    `user_id`     BIGINT NOT NULL COMMENT '所属用户ID',
    `role`        ENUM('USER','ASSISTANT') NOT NULL COMMENT '消息角色',
    `content`     TEXT NOT NULL COMMENT '消息内容',
    `task_id`     BIGINT DEFAULT NULL COMMENT '关联的生成任务ID',
    `version_id`  BIGINT DEFAULT NULL COMMENT '关联的版本ID（成功时关联）',
    `created_at`  BIGINT(20) NOT NULL DEFAULT 0 COMMENT '创建时间戳（毫秒）',
    INDEX `idx_app_id` (`app_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话消息表';