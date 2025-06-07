-- 建立資料庫並切換
CREATE DATABASE IF NOT EXISTS apisix;
USE apisix;

-- 1. Route 模板表：定義共用路由配置，支援 Pebble 模板渲染
CREATE TABLE IF NOT EXISTS route_templates (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,         -- 模板代碼，如: ai-dynamic
    description VARCHAR(255),
    route_template TEXT NOT NULL,             -- 支援 Pebble 語法
    plugin_template TEXT NOT NULL,            -- JSON 格式，包含 Pebble 條件（非純 JSON）
    vars_template JSON NOT NULL,              -- personaType 對應 vars 條件，e.g. {"tenant": [[...]]}
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Upstream 模板表：支援 Pebble 語法渲染，動態生成 upstream 結構
CREATE TABLE IF NOT EXISTS upstream_templates (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,         -- 模板代碼，如: basic-upstream
    description VARCHAR(255),
    upstream_template TEXT NOT NULL,          -- 支援 Pebble 渲染
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. API 定義表：每筆 API 使用一組 route 與 upstream 模板組合
CREATE TABLE IF NOT EXISTS api_definitions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE,             -- API 名稱，如 getUserInfo
    uri VARCHAR(255) NOT NULL,
    service_name VARCHAR(64) NOT NULL,
    route_template_code VARCHAR(64) NOT NULL,
    upstream_template_code VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (route_template_code) REFERENCES route_templates(code) ON DELETE RESTRICT,
    FOREIGN KEY (upstream_template_code) REFERENCES upstream_templates(code) ON DELETE RESTRICT
);

-- 4. API 訂閱記錄表：記錄使用者申請的 API 與訂閱變數與 context
CREATE TABLE IF NOT EXISTS api_subscriptions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_name VARCHAR(64) NOT NULL,              -- 使用者帳號
    persona_type VARCHAR(64) NOT NULL,           -- 如 tenant, provider
    api_id INT NOT NULL,                         -- 對應 api_definitions.id
    route_id VARCHAR(255) NOT NULL,              -- 生成的 APISIX route id
    upstream_id VARCHAR(255) NOT NULL,           -- 生成的 APISIX upstream id
    subscribed_vars JSON NOT NULL,               -- 渲染後的 vars 結構
    template_context JSON NOT NULL,              -- 渲染用變數，如 userName、uri、upstream_id 等
    subscribed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (api_id) REFERENCES api_definitions(id) ON DELETE CASCADE
);
