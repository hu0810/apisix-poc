CREATE DATABASE IF NOT EXISTS apisix;
USE apisix;

-- 1. Route 模板：共用一筆，可根據 context 渲染 route/plugin/vars
CREATE TABLE IF NOT EXISTS route_templates (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    description VARCHAR(255),
    route_template TEXT NOT NULL,      -- 含 uri, methods, upstream_id
    plugin_template TEXT NOT NULL,     -- 含 dynamic-upstream 的配置
    vars_template TEXT NOT NULL        -- JSON object: personaType -> List of conditions
);

-- 2. API 定義：每筆 API 使用一筆模板
CREATE TABLE IF NOT EXISTS api_definitions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE,             -- 如 getUserInfo
    uri VARCHAR(255) NOT NULL,             -- 如 /user/info
    service_name VARCHAR(64) NOT NULL,     -- 如 user-service
    route_template_code VARCHAR(64) NOT NULL,
    FOREIGN KEY (route_template_code) REFERENCES route_templates(code)
);

-- 3. API 綁定記錄：記錄用戶申請過哪些 API 與當下 context + vars（執行紀錄）
CREATE TABLE IF NOT EXISTS api_bindings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_name VARCHAR(64) NOT NULL,
    persona_type VARCHAR(64) NOT NULL,
    api_name VARCHAR(64) NOT NULL,
    bound_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    bound_vars TEXT NOT NULL,              -- 渲染後的 vars
    template_context TEXT NOT NULL         -- context: userName, personaType, url, upstream_id...
);
