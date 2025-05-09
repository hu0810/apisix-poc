-- API 定義
INSERT INTO api_definitions (name, uri, service_name)
VALUES ('apiA', '/api/a', 'serviceA');

-- plugin 綁定
INSERT INTO api_plugin_bindings (api_id, plugin_name, config_template)
VALUES
(1, 'jwt-auth', '{}'),
(1, 'custom-auth', '{"persona_type": "{{personaType}}", "app": "{{appName}}"}');

-- plugin 需要的變數
INSERT INTO plugin_variable_defs (plugin_name, variable_name, required)
VALUES
('custom-auth', 'persona_type', TRUE),
('custom-auth', 'app', TRUE);

-- upstream 模板
INSERT INTO api_upstream_templates (service_name, upstream_json)
VALUES
('serviceA', '{
    "type": "roundrobin",
    "nodes": { "127.0.0.1:9000": 1 },
    "pass_host": "rewrite",
    "scheme": "http"
}');
