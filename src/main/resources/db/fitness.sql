-- ============================================
-- AI 健身计划助手 - 数据库建表 SQL
-- ============================================

-- 用户表
CREATE TABLE users (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password    VARCHAR(255) NOT NULL COMMENT 'BCrypt 加密后的密码',
    nickname    VARCHAR(50) COMMENT '昵称',
    avatar_url  VARCHAR(500) COMMENT '头像地址',
    status      TINYINT NOT NULL DEFAULT 1 COMMENT '1=正常 0=禁用',
    deleted     TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at  BIGINT NOT NULL COMMENT '创建时间戳(毫秒)',
    updated_at  BIGINT NOT NULL COMMENT '更新时间戳(毫秒)'
);

-- 用户健身偏好表
CREATE TABLE user_preferences (
    id                      BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id                 BIGINT NOT NULL COMMENT '关联 users.id',
    experience_level        VARCHAR(20) NOT NULL COMMENT 'beginner/intermediate/advanced',
    goal                    VARCHAR(30) NOT NULL COMMENT 'muscle_gain/fat_loss/body_shaping/endurance/general_health',
    focus_muscles           JSON COMMENT '多选部位，如 ["chest","back","arms"]',
    equipment               VARCHAR(30) NOT NULL COMMENT 'none/dumbbell/barbell/gym_machine/home_equipment',
    session_duration        VARCHAR(20) NOT NULL COMMENT 'under_30/30_to_60/60_to_90/above_90',
    training_days_per_week  INT NOT NULL COMMENT '每周训练天数 2-6',
    training_style          VARCHAR(20) NOT NULL COMMENT 'split/full_body/hiit/circuit/strength',
    injury_notes            TEXT COMMENT '受伤或需规避部位说明',
    deleted                 TINYINT NOT NULL DEFAULT 0,
    created_at              BIGINT NOT NULL COMMENT '创建时间戳(毫秒)',
    updated_at              BIGINT NOT NULL COMMENT '更新时间戳(毫秒)'
);

-- AI 生成的周计划表
CREATE TABLE weekly_plans (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL,
    preference_id   BIGINT NOT NULL,
    title           VARCHAR(100) NOT NULL COMMENT 'AI 生成的计划标题',
    summary         TEXT COMMENT 'AI 生成的整体说明',
    week_start_date DATE NOT NULL COMMENT '计划开始日期',
    status          VARCHAR(20) NOT NULL DEFAULT 'generating' COMMENT 'generating/done/failed',
    deleted         TINYINT NOT NULL DEFAULT 0,
    created_at      BIGINT NOT NULL COMMENT '创建时间戳(毫秒)',
    updated_at      BIGINT NOT NULL COMMENT '更新时间戳(毫秒)'
);

-- 每日训练表
CREATE TABLE daily_trainings (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id             BIGINT NOT NULL,
    day_of_week         INT NOT NULL COMMENT '1=周一 … 7=周日',
    is_rest_day         TINYINT NOT NULL DEFAULT 0,
    focus_muscle_group  VARCHAR(100) COMMENT '如：胸肌+三头肌',
    warm_up_notes       TEXT,
    cool_down_notes     TEXT,
    created_at          BIGINT NOT NULL COMMENT '创建时间戳(毫秒)',
    updated_at          BIGINT NOT NULL COMMENT '更新时间戳(毫秒)'
);

-- 每日具体动作表
CREATE TABLE daily_exercise_items (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    daily_training_id   BIGINT NOT NULL,
    name_zh             VARCHAR(100) NOT NULL COMMENT '动作中文名',
    name_en             VARCHAR(100) NOT NULL COMMENT '动作英文名',
    sort_order          INT NOT NULL DEFAULT 0,
    sets                INT COMMENT '组数',
    reps                VARCHAR(20) COMMENT '次数或时长，如 10-12 或 30s',
    rest_seconds        INT COMMENT '组间休息秒数',
    coach_notes         TEXT COMMENT 'AI 生成的动作要点',
    bilibili_url        VARCHAR(500) COMMENT 'B站搜索跳转链接，写入时自动拼接',
    created_at          BIGINT NOT NULL COMMENT '创建时间戳(毫秒)',
    updated_at          BIGINT NOT NULL COMMENT '更新时间戳(毫秒)'
);
