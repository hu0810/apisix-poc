-- 1. 定義 API 基本資訊
CREATE TABLE api_definitions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,          -- apiA, apiB...
    uri VARCHAR(255) NOT NULL,          -- 如: /apiA
    service_name VARCHAR(64) NOT NULL   -- 可對應 upstream
);

-- 2. API 需要掛的 Plugin 與對應的變數模板
CREATE TABLE api_plugin_bindings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    api_id INT NOT NULL,
    plugin_name VARCHAR(64) NOT NULL,            -- 如: custom-auth
    config_template TEXT NOT NULL,               -- 如: {"persona_type": "{{personaType}}"}
    FOREIGN KEY (api_id) REFERENCES api_definitions(id)
);

-- 3. 每個 plugin 需要的變數（用於驗證前端是否給足資料）
CREATE TABLE plugin_variable_defs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    plugin_name VARCHAR(64) NOT NULL,
    variable_name VARCHAR(64) NOT NULL,
    required BOOLEAN DEFAULT TRUE
);

-- 4. 模擬 upstream 配置模板
CREATE TABLE api_upstream_templates (
    id INT AUTO_INCREMENT PRIMARY KEY,
    service_name VARCHAR(64) NOT NULL,  -- 對應 api_definitions.service_name
    upstream_json TEXT NOT NULL         -- JSON 格式 upstream 結構
);
