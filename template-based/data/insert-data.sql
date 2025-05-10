
-- INSERT INTO upstreams (id, name, type, nodes) VALUES
-- (1, 'user-service', 'roundrobin', '{"127.0.0.1:9000": 1}'),
-- (2, 'order-service', 'roundrobin', '{"127.0.0.1:9001": 1}');


INSERT INTO services (id, name, upstream_id) VALUES
(1, 'user-api', 1),
(2, 'order-api', 2);


INSERT INTO routes (id, uri, methods, vars, service_id) VALUES
(1, '/user/info', '["GET"]', '[["http_appname", "==", "app1"]]', 1),
(2, '/user/info', '["GET"]', '[["http_appname", "==", "app2"]]', 1),
(3, '/order/list', '["POST"]', '[["http_appname", "==", "app1"]]', 2);


INSERT INTO plugin_templates (id, name, plugin_name, config_template) VALUES
(1, 'limit_by_app', 'limit-count', '{"count": "$count", "time_window": 60, "key": "$key", "rejected_code": 503}'),
(2, 'auth_by_app', 'jwt-auth', '{"key": "$jwt_key", "secret": "$jwt_secret"}');


INSERT INTO plugin_instances (id, plugin_template_id, bound_type, bound_id, variables) VALUES
(1, 1, 'route', 1, '{"count": 50, "key": "http_appname"}'),
(2, 1, 'route', 2, '{"count": 100, "key": "http_appname"}'),
(3, 2, 'route', 1, '{"jwt_key": "key_app1", "jwt_secret": "secret_app1"}'),
(4, 2, 'route', 2, '{"jwt_key": "key_app2", "jwt_secret": "secret_app2"}'),
(5, 1, 'route', 3, '{"count": 30, "key": "http_appname"}'),
(6, 2, 'route', 3, '{"jwt_key": "key_app1", "jwt_secret": "secret_app1"}');


INSERT INTO consumers (id, username, vars, plugins) VALUES
(1, 'client_app1', '{"http_appname": "app1"}', NULL),
(2, 'client_app2', '{"http_appname": "app2"}', NULL);



INSERT INTO consumers (id, username, vars, plugins) VALUES
(3, 'client_app3', '{"http_appname": "app3", "http_region": "tw"}',
 '{"key-auth": {"key": "client_app3_key"}}');


INSERT INTO upstreams (
    id, name, type, scheme, pass_host, upstream_host, keepalive,
    timeout, nodes, checks
) VALUES (
    1,
    'my-upstream',
    'roundrobin',
    'http',
    'rewrite',
    'backend.example.com',
    60,
    '{"connect": 3, "send": 5, "read": 10}',
    '[{"host": "192.168.0.10", "port": 8080, "weight": 1}, {"host": "192.168.0.11", "port": 8080, "weight": 1}]',
    '{
        "active": {
            "http_path": "/status",
            "host": "backend.example.com",
            "port": 8080,
            "healthy": {
                "interval": 5,
                "successes": 2
            },
            "unhealthy": {
                "interval": 5,
                "http_failures": 3,
                "tcp_failures": 2,
                "timeouts": 2
            }
        },
        "passive": {
            "healthy": {
                "http_statuses": [200, 302],
                "successes": 3
            },
            "unhealthy": {
                "http_statuses": [500, 503],
                "http_failures": 2,
                "tcp_failures": 2,
                "timeouts": 2
            }
        }
    }'
);



INSERT INTO services (id, name, upstream_id) VALUES
(3, 'report-api', 3);


INSERT INTO routes (id, uri, methods, vars, service_id) VALUES
(4, '/report/data', '["GET"]', '[["http_appname", "==", "app3"], ["http_region", "==", "tw"]]', 3);


INSERT INTO plugin_templates (id, name, plugin_name, config_template) VALUES
(3, 'auth_key_template', 'key-auth', '{"key": "$key"}');


INSERT INTO plugin_instances (id, plugin_template_id, bound_type, bound_id, variables) VALUES
(7, 3, 'consumer', 3, '{"key": "client_app3_key"}');


INSERT INTO plugin_instances (id, plugin_template_id, bound_type, bound_id, variables) VALUES
(8, 1, 'route', 4, '{"count": 20, "key": "http_appname"}'),
(9, 2, 'route', 4, '{"jwt_key": "key_app3", "jwt_secret": "secret_app3"}');


INSERT INTO plugin_templates (id, name, plugin_name, config_template) VALUES
(4, 'beta_auth_template', 'beta_auth', '{"personaType": "$personaType", "beta_key": "$beta_key"}'),
(5, 'alpha_auth_template', 'alpha_auth', '{"key": "$key"}');


INSERT INTO consumers (id, username, vars, plugins) VALUES
(4, 'consumer_beta', '{"http_appname": "beta_app"}', NULL),
(5, 'consumer_alpha', '{"http_appname": "alpha_app"}', NULL),
(6, 'consumer_both', '{"http_appname": "both_app"}', NULL);


INSERT INTO plugin_instances (id, plugin_template_id, bound_type, bound_id, variables) VALUES
(10, 4, 'consumer', 4, '{"personaType": "tenant", "beta_key": "beta_secret_123"}'),
(11, 5, 'consumer', 5, '{"key": "alpha_secret_456"}'),
(12, 4, 'consumer', 6, '{"personaType": "provider", "beta_key": "beta_secret_789"}'),
(13, 5, 'consumer', 6, '{"key": "alpha_secret_999"}');



-- Sample data for jwt-auth (no user input needed, system/consumer defined)
INSERT INTO plugin_template_variables (plugin_template_id, variable_name, source)
VALUES
(2, 'jwt_key', 'consumer_var'),
(2, 'jwt_secret', 'consumer_var');

-- Sample data for limit-count (key from consumer, count is system)
INSERT INTO plugin_template_variables (plugin_template_id, variable_name, source, default_value)
VALUES
(1, 'key', 'consumer_var', NULL),
(1, 'count', 'system_fixed', '100');

-- Sample data for beta_auth (user must provide personaType and beta_key)
INSERT INTO plugin_template_variables (plugin_template_id, variable_name, source)
VALUES
(4, 'personaType', 'user_input'),
(4, 'beta_key', 'user_input');

-- Sample data for alpha_auth (user must provide key)
INSERT INTO plugin_template_variables (plugin_template_id, variable_name, source)
VALUES
(5, 'key', 'user_input');

