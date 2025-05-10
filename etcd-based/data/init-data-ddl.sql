-- 建立資料庫並切換
CREATE DATABASE IF NOT EXISTS apisix;
USE apisix;

-- 1. Route 模板表：定義共用路由配置，支援 Pebble 模板渲染
CREATE TABLE IF NOT EXISTS route_templates (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,         -- 模板代碼，如: ai-dynamic
    description VARCHAR(255),                 -- 模板說明
    route_template TEXT NOT NULL,             -- route 結構 (e.g. uri, methods, upstream_id)
    plugin_template TEXT NOT NULL,            -- plugin 結構 (e.g. custom-auth, limit-count)
    vars_template TEXT NOT NULL               -- JSON：personaType -> List of var 條件
);

-- 2. Upstream 模板表：支援 Pebble 語法渲染，動態生成 upstream 結構
CREATE TABLE IF NOT EXISTS upstream_templates (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,         -- 模板代碼，如: basic-upstream
    description VARCHAR(255),                 -- 模板說明
    upstream_template TEXT NOT NULL           -- upstream JSON 結構，支援 Pebble 語法
);

-- 3. API 定義表：每筆 API 使用一組 route 與 upstream 模板組合
CREATE TABLE IF NOT EXISTS api_definitions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE,             -- API 名稱，如 getUserInfo
    uri VARCHAR(255) NOT NULL,                    -- 路由 URI，如 /user/info
    service_name VARCHAR(64) NOT NULL,            -- 所屬服務名，如 user-service
    route_template_code VARCHAR(64) NOT NULL,     -- 對應 route_templates.code
    upstream_template_code VARCHAR(64) NOT NULL,  -- 對應 upstream_templates.code,
    FOREIGN KEY (route_template_code) REFERENCES route_templates(code),
    FOREIGN KEY (upstream_template_code) REFERENCES upstream_templates(code)
);

-- 4. API 綁定記錄表：記錄使用者申請的 API 與綁定變數與 context
CREATE TABLE IF NOT EXISTS api_bindings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_name VARCHAR(64) NOT NULL,          -- 使用者帳號
    persona_type VARCHAR(64) NOT NULL,       -- 如 tenant, provider
    api_name VARCHAR(64) NOT NULL,           -- 綁定的 API 名稱
    bound_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    bound_vars TEXT NOT NULL,                -- 渲染後的 vars JSON
    template_context TEXT NOT NULL           -- context 變數（如 userName, uri, upstream_id 等）
);
