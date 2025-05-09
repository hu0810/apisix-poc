INSERT INTO api_templates (resource_type, json_template) VALUES
('route',    '{
  "id": "tenant_${persona}_${api}",
  "uri": "/${api}/*",
  "vars": [["http_appname","==","${persona}"]],
  "upstream_id": "${api}_up",
  "plugins": {
    "jwt-auth": {},
    "acl": { "allow": ["${persona}"] }
  }
}'),
('consumer', '{
  "username": "${persona}",
  "plugins": {
    "jwt-auth": { "secret": "${jwt_secret}" },
    "acl": { "group": ["${persona}"] }
  }
}');

-- persona 變數：tenant=acme
INSERT INTO persona_variables (persona_type, var_key, var_value)
VALUES ('tenant', 'jwt_secret', 'acmeSecret123');

-- api 變數：orders_upstream
INSERT INTO api_variables (api_id, var_key, var_value)
VALUES (1, 'api', 'orders');
SQL