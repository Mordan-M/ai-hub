-- 低代码平台数据库表结构
-- 字符集: utf8mb4

-- 1. 应用表
CREATE TABLE IF NOT EXISTS `application` (
    `id`            BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id`       BIGINT NOT NULL COMMENT '所属用户ID',
    `name`          VARCHAR(100) NOT NULL COMMENT '应用名称',
    `description`   TEXT COMMENT '应用描述',
    `status`        TINYINT(4) NOT NULL DEFAULT 0,
    `thumbnail_url` VARCHAR(500) COMMENT '预览缩略图地址',
    `created_at`    BIGINT(20) NOT NULL DEFAULT 0 COMMENT '创建时间戳（毫秒）',
    `updated_at`    BIGINT(20) NOT NULL DEFAULT 0 COMMENT '更新时间戳（毫秒）',
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='低代码应用表';

-- 移除版本概念后变更:
-- ALTER TABLE `application` DROP COLUMN `latest_version_id`;

-- 2. 生成任务表
CREATE TABLE IF NOT EXISTS `generation_task` (
    `id`                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    `app_id`              BIGINT NOT NULL COMMENT '所属应用ID',
    `user_id`             BIGINT NOT NULL COMMENT '所属用户ID',
    `prompt`              TEXT NOT NULL COMMENT '用户输入提示词',
    `api_doc_text`        TEXT COMMENT '用户提供的API文档文本（可为空）',
    `status`              TINYINT(4) NOT NULL DEFAULT 0,
    `retry_count`         INT NOT NULL DEFAULT 0 COMMENT '已重试次数',
    `max_retry`           INT NOT NULL DEFAULT 3 COMMENT '最大重试次数',
    `workflow_thread_id`  VARCHAR(100) COMMENT 'LangGraph4j threadId',
    `error_message`       TEXT COMMENT '失败原因',
    `created_at`          BIGINT(20) NOT NULL DEFAULT 0 COMMENT '创建时间戳（毫秒）',
    `finished_at`         BIGINT(20) DEFAULT NULL COMMENT '完成时间戳（毫秒）',
    INDEX `idx_app_id` (`app_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码生成任务表';

-- 移除版本概念后变更:
-- ALTER TABLE `generation_task` DROP COLUMN `parent_version_id`;

-- 3. 生成记录表（每次生成都保留一条记录）
CREATE TABLE IF NOT EXISTS `generated_record` (
    `id`                BIGINT AUTO_INCREMENT PRIMARY KEY,
    `app_id`            BIGINT NOT NULL COMMENT '所属应用ID',
    `task_id`           BIGINT NOT NULL COMMENT '对应生成任务ID',
    `file_prefix`       VARCHAR(20) COMMENT '项目文件存储前缀（随机生成，用于隔离不同构建）',
    `code_storage_path` VARCHAR(500) COMMENT '代码文件存储路径',
    `preview_url`       VARCHAR(500) COMMENT '预览/部署地址',
    `download_url`      VARCHAR(500) COMMENT '下载地址',
    `file_size`         BIGINT COMMENT '代码包大小（字节）',
    `prompt_snapshot`   TEXT COMMENT '生成时的完整提示词快照',
    `project_summary`   TEXT COMMENT '项目文件摘要（大模型返回，简要描述每个文件作用）',
    `deploy_url`        VARCHAR(500) COMMENT '部署后访问URL（用户部署时写入）',
    `created_at`        BIGINT(20) NOT NULL DEFAULT 0 COMMENT '创建时间戳（毫秒）',
    `updated_at`        BIGINT(20) NOT NULL DEFAULT 0 COMMENT '更新时间戳（毫秒）',
    INDEX `idx_app_id` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生成代码记录表（每次生成保留一条历史记录）';

-- 4. 对话消息表
CREATE TABLE IF NOT EXISTS `conversation_message` (
    `id`           BIGINT AUTO_INCREMENT PRIMARY KEY,
    `app_id`       BIGINT NOT NULL COMMENT '所属应用ID',
    `user_id`      BIGINT NOT NULL COMMENT '所属用户ID',
    `seq`          INT NOT NULL COMMENT '消息顺序号',
    `role`         TINYINT(4) NOT NULL COMMENT '消息角色',
    `content`      TEXT NOT NULL COMMENT '消息内容（纯文本，用户输入或AI回复）',
    `task_id`      BIGINT DEFAULT NULL COMMENT '关联的生成任务ID',
    `created_at`   BIGINT(20) NOT NULL DEFAULT 0 COMMENT '创建时间戳（毫秒）',
    INDEX `idx_app_id_seq` (`app_id`, `seq`),
    INDEX `idx_user_id`    (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话消息表';

-- 5. LangChain4j 对话记忆表（专门存储AI对话记忆，与业务消息分离）
CREATE TABLE IF NOT EXISTS `chat_memory` (
    `id`           BIGINT AUTO_INCREMENT PRIMARY KEY,
    `memory_id`    VARCHAR(100) NOT NULL COMMENT '记忆ID（通常为 appId）',
    `seq`          INT NOT NULL COMMENT '消息顺序号',
    `role`         TINYINT(4) NOT NULL COMMENT '消息角色',
    `content`      TEXT NOT NULL COMMENT 'LangChain4j ChatMessage 序列化 JSON',
    `content_text` TEXT DEFAULT NULL COMMENT '纯文本内容',
    `created_at`   BIGINT(20) NOT NULL DEFAULT 0 COMMENT '创建时间戳（毫秒）',
    INDEX `idx_memory_id_seq` (`memory_id`, `seq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LangChain4j 对话记忆表';
