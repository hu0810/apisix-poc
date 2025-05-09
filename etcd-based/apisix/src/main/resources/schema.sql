-- APISIX template & variable schema for MariaDB
CREATE TABLE IF NOT EXISTS api_templates (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  resource_type ENUM('route','service','upstream','consumer') NOT NULL,
  json_template TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS plugin_templates (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  plugin_name VARCHAR(128) NOT NULL,
  json_template TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS persona_variables (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  persona_type VARCHAR(64) NOT NULL,
  var_key VARCHAR(128) NOT NULL,
  var_value VARCHAR(1024) NOT NULL,
  UNIQUE KEY uniq_persona_var (persona_type, var_key)
);

CREATE TABLE IF NOT EXISTS api_variables (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  api_id BIGINT NOT NULL,
  var_key VARCHAR(128) NOT NULL,
  var_value VARCHAR(1024) NOT NULL,
  UNIQUE KEY uniq_api_var (api_id, var_key)
);
