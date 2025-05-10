USE apisix;

INSERT INTO route_templates (code, description, route_template, plugin_template, vars_template) VALUES
('persona-basic', '通用身份驗證 API，支援多 personaType',
 '{"uri": "{{uri}}", "name": "{{userName}}-{{name}}", "methods": ["GET"], "upstream": {"type": "roundrobin", "nodes": {"127.0.0.1:9000": 1}}}',
 '{"custom-auth": {"persona_type": "{{type}}", "user": "{{userName}}"}}',
 '{
   \"tenant\": [
     [\"http_type\", \"==\", \"tenant\"],
     [\"http_appname\", \"==\", \"{{userName}}\"]
   ],
   \"provider\": [
     [\"http_type\", \"==\", \"provider\"],
     [\"http_key\", \"==\", \"{{userName}}\"]
   ],
   \"alpha\": [
     [\"http_alpha_appname\", \"==\", \"{{userName}}\"]
   ]
 }'
);

-- 插入共用 route_template，適用所有 AI API
INSERT INTO route_templates (
    code, description, route_template, plugin_template, vars_template
) VALUES (
    'ai-dynamic',
    'AI 服務使用 dynamic-upstream plugin，自動建立 upstream 並注入 URL',

    -- route_template
    '{\"uri\": \"{{uri}}\", \"name\": \"{{userName}}-{{name}}\", \"methods\": [\"POST\"], \"upstream_id\": \"{{upstream_id}}\"}',

    -- plugin_template
    '{ 
        \"dynamic-upstream\": {
            \"persona_type\": \"{{personaType}}\",
            \"user\": \"{{userName}}\",
            \"url\": \"{{url}}\",
            \"upstream_id\": \"{{upstream_id}}\"
        }
    }',

    -- vars_template
    '{ 
        \"provider\": [
            [\"http_personaType\", \"==\", \"provider\"],
            [\"http_providerKey\", \"==\", \"{{userName}}\"]
        ],
        \"tenant\": [
            [\"http_personaType\", \"==\", \"tenant\"],
            [\"http_appname\", \"==\", \"{{serviceName}}\"]
        ]
    }'
);





INSERT INTO route_templates (
  code, description, route_template, plugin_template, vars_template
) VALUES (
  'auth-limit',
  'auth with optional rate limit',
  '{\"uri\": \"{{uri}}\", \"name\": \"{{userName}}-{{name}}\", \"methods\": [\"GET\"], \"upstream_id\": \"{{upstream_id}}\"}',
  '{
    \"custom-auth\": {
      \"persona_type\": \"{{ personaType }}\",
      \"user\": \"{{ userName }}\"
    }
    {% if count and time_window %},
    \"limit-count\": {
      \"count\": {{ count }},
      \"time_window\": {{ time_window }},
      \"key\": \"remote_addr\",
      \"policy\": \"local\"
    }
    {% endif %}
  }',
  '{
    \"provider\": [
      [\"http_personaType\", \"==\", \"provider\"],
      [\"http_providerKey\", \"==\", \"{{userName}}\"]

    ],
    \"tenant\": [
      [\"http_personaType\", \"==\", \"tenant\"],
      [\"http_appname\", \"==\", \"{{serviceName}}\"]

    ]
  }'
);

INSERT INTO api_definitions (name, uri, service_name, route_template_code) VALUES
('getUserInfo', '/user/info', 'user-service', 'persona-basic'),
('generateImage', '/ai/image', 'ai-service', 'persona-basic'),
('generateImageDynamic', '/ai/image', 'ai-service', 'ai-dynamic'),
('summarizeText', '/ai/summarize', 'ai-service', 'ai-dynamic'),
('limitCountTest', '/ai/summarize', 'ai-service', 'auth-limit');


