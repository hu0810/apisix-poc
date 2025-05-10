-- Routes
CREATE TABLE routes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    uri VARCHAR(255) NOT NULL,
    methods JSON DEFAULT NULL,
    vars JSON DEFAULT NULL, -- e.g., [["http_appname", "==", "app1"], ...]
    service_id INT,
    status TINYINT DEFAULT 1,
    UNIQUE KEY (uri, vars(255))
);

-- Services
CREATE TABLE services (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    upstream_id INT
);

-- Upstreams
CREATE TABLE upstreams (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,           -- e.g., roundrobin
    scheme VARCHAR(10) DEFAULT 'http',   -- e.g., http, https
    pass_host VARCHAR(20) DEFAULT 'pass',-- e.g., pass, node, rewrite
    upstream_host VARCHAR(255) DEFAULT NULL,
    keepalive INT DEFAULT NULL,
    timeout JSON DEFAULT NULL,           -- {"connect": 3, "send": 5, "read": 10}
    nodes JSON NOT NULL,                 -- [{"host": "192.168.0.10", "port": 8080, "weight": 1}, ...]
    checks JSON DEFAULT NULL             -- full active/passive health check JSON
);


-- Plugin templates (可共用的 plugin 結構)
CREATE TABLE plugin_templates (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    plugin_name VARCHAR(100) NOT NULL,
    config_template JSON NOT NULL -- e.g., {"limit-count": {"count": "$count", "key": "$key"}}
);

-- Plugin instances (實際使用的 plugins)
CREATE TABLE plugin_instances (
    id INT AUTO_INCREMENT PRIMARY KEY,
    plugin_template_id INT,
    bound_type ENUM('route', 'service', 'consumer') NOT NULL,
    bound_id INT NOT NULL, -- route.id or service.id
    variables JSON NOT NULL -- e.g., {"count": 100, "key": "http_appname"}
);

-- Consumers
CREATE TABLE consumers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    vars JSON NOT NULL, -- e.g., {"http_appname": "app1"}
    plugins JSON DEFAULT NULL -- 如需指定 consumer plugin
);


-- DDL for plugin_template_variables
CREATE TABLE plugin_template_variables (
    id INT AUTO_INCREMENT PRIMARY KEY,
    plugin_template_id INT NOT NULL,
    variable_name VARCHAR(100) NOT NULL,
    source ENUM('user_input', 'consumer_var', 'system_fixed', 'inferred') NOT NULL,
    default_value TEXT DEFAULT NULL
);

