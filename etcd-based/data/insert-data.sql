use apisix;

INSERT INTO route_templates (
    code, description, route_template, plugin_template, vars_template
) VALUES
-- persona-basic
('persona-basic',
 '通用身份驗證 API，支援多 personaType',
 '{"uri": "{{uri}}", "name": "{{userName}}-{{name}}", "methods": ["GET"], "upstream_id": "common-upstream"}',
 '{
    "custom-auth": {
        "persona_type": "{{personaType}}",
        "user": "{{userName}}"
    }
 }',
 '{
    "tenant": [
        ["http_type", "==", "tenant"],
        ["http_appname", "==", "{{userName}}"]
    ],
    "provider": [
        ["http_type", "==", "provider"],
        ["http_key", "==", "{{userName}}"]
    ],
    "alpha": [
        ["http_alpha_appname", "==", "{{userName}}"]
    ]
 }'),

-- ai-dynamic
('ai-dynamic',
 'AI 服務使用 dynamic-upstream plugin，自動建立 upstream 並注入 URL',
 '{"uri": "{{uri}}", "name": "{{userName}}-{{name}}", "methods": ["POST"]}',
 '{
    "dynamic-upstream": {
        "persona_type": "{{personaType}}",
        "user": "{{userName}}",
        "url": "{{url}}",
        "upstream_id": "{{upstream_id}}"
    }
 }',
 '{
    "provider": [
        ["http_personaType", "==", "provider"],
        ["http_providerKey", "==", "{{userName}}"]
    ],
    "tenant": [
        ["http_personaType", "==", "tenant"],
        ["http_appname", "==", "{{serviceName}}"]
    ]
 }'),

-- auth-limit（count 與 time_window 必須同時存在）
('auth-limit',
 'auth with optional rate limit（count 與 time_window 必須成對出現）',
 '{"uri": "{{uri}}", "name": "{{userName}}-{{name}}", "methods": ["GET"]}',
 '{
    "custom-auth": {
        "persona_type": "{{personaType}}",
        "user": "{{userName}}"
    }{{limit_count_plugin}}
 }',
 '{
    "provider": [
        ["http_personaType", "==", "provider"],
        ["http_providerKey", "==", "{{userName}}"]
    ],
    "tenant": [
        ["http_personaType", "==", "tenant"],
        ["http_appname", "==", "{{serviceName}}"]
    ]
 }');


-- 插入 upstream_templates（保持不變）
INSERT INTO upstream_templates (code, description, upstream_template) VALUES
('common-upstream',
 '通用 Upstream 設定，固定 timeout、keepalive_pool、hash_on、nodes',
 '{
    "id": "common-upstream",
    "type": "roundrobin",
    "scheme": "http",
    "pass_host": "rewrite",
    "upstream_host": "common-upstream.com",
    "hash_on": "vars",
    "timeout": {
        "connect": 30,
        "send": 30,
        "read": 30
    },
    "keepalive_pool": {
        "idle_timeout": 60,
        "requests": 1000000,
        "size": 5000
    },
    "nodes": [
        {
            "host": "common-upstream.com",
            "port": 80,
            "weight": 1
        }
    ]
 }'),
('openai-upstream',
 'OpenAI 專用 Upstream，帶有使用者動態 ID',
 '{
    "id": "{{upstream_id}}",
    "type": "roundrobin",
    "scheme": "http",
    "pass_host": "rewrite",
    "upstream_host": "{{upstream_host}}",
    "hash_on": "vars",
    "timeout": {
        "connect": 30,
        "send": 30,
        "read": 30
    },
    "keepalive_pool": {
        "idle_timeout": 60,
        "requests": 1000000,
        "size": 5000
    },
    "nodes": {{nodes_json}}
}');

-- 插入 api_definitions（保持不變）
INSERT INTO api_definitions (
    name, uri, service_name, route_template_code, upstream_template_code
) VALUES
('getUserInfo', '/user/info', 'user-service', 'persona-basic', 'common-upstream'),
('generateImage', '/ai/image', 'ai-service', 'persona-basic', 'common-upstream'),
('generateImageDynamic', '/ai/image', 'ai-service', 'ai-dynamic', 'openai-upstream'),
('summarizeText', '/ai/summarize', 'ai-service', 'ai-dynamic', 'openai-upstream'),
('limitCountTest', '/ai/summarize', 'ai-service', 'auth-limit', 'openai-upstream');
