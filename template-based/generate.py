import mysql.connector
import yaml
from collections import defaultdict

DB_CONFIG = {
    'host': '127.0.0.1',
    'port': 3306,
    'user': 'user',
    'password': 'password',
    'database': 'demo'
}

def generate_plugin_config(template_str, variables):
    template = yaml.safe_load(template_str)
    def replace_vars(obj):
        if isinstance(obj, dict):
            return {k: replace_vars(v) for k, v in obj.items()}
        elif isinstance(obj, list):
            return [replace_vars(i) for i in obj]
        elif isinstance(obj, str) and obj.startswith('$'):
            return variables.get(obj[1:], obj)
        return obj
    return replace_vars(template)

def main():
    conn = mysql.connector.connect(**DB_CONFIG)
    cursor = conn.cursor(dictionary=True)

    # Load upstreams
    cursor.execute("SELECT * FROM upstreams")
    upstreams = []
    for row in cursor.fetchall():
        upstream = {
            "id": f"upstream_{row['id']}",
            "name": row["name"],
            "type": row["type"],
            "scheme": row.get("scheme", "http"),
            "pass_host": row.get("pass_host", "pass"),
            "upstream_host": row.get("upstream_host"),
            "keepalive": row.get("keepalive"),
            "timeout": yaml.safe_load(row["timeout"]) if row["timeout"] else None,
            "nodes": yaml.safe_load(row["nodes"]),
            "checks": yaml.safe_load(row["checks"]) if row["checks"] else None
        }
        upstream = {k: v for k, v in upstream.items() if v is not None}
        upstreams.append(upstream)

    # Load services
    cursor.execute("SELECT * FROM services")
    services = [{
        "id": f"service_{row['id']}",
        "name": row["name"],
        "upstream_id": f"upstream_{row['upstream_id']}"
    } for row in cursor.fetchall()]

    # Load plugin templates
    cursor.execute("SELECT * FROM plugin_templates")
    plugin_templates = {row['id']: row for row in cursor.fetchall()}

    # Load plugin instances
    cursor.execute("SELECT * FROM plugin_instances")
    plugin_map = defaultdict(list)
    for row in cursor.fetchall():
        template = plugin_templates[row['plugin_template_id']]
        config = generate_plugin_config(template['config_template'], yaml.safe_load(row['variables']))
        plugin_map[(row['bound_type'], row['bound_id'])].append({template['plugin_name']: config})

    # Load routes
    cursor.execute("SELECT * FROM routes")
    routes = []
    for row in cursor.fetchall():
        route_plugins = plugin_map.get(('route', row['id']), [])
        route = {
            "uri": row["uri"],
            "methods": yaml.safe_load(row["methods"]) if row["methods"] else None,
            "vars": yaml.safe_load(row["vars"]) if row["vars"] else None,
            "service_id": f"service_{row['service_id']}",
        }
        if route_plugins:
            route["plugins"] = {k: v for plugin in route_plugins for k, v in plugin.items()}
        routes.append(route)

    # Load consumers
    cursor.execute("SELECT * FROM consumers")
    consumers = []
    for row in cursor.fetchall():
        consumer_plugins = plugin_map.get(('consumer', row['id']), [])
        consumer = {
            "username": row["username"],
            "vars": yaml.safe_load(row["vars"]),
        }
        if consumer_plugins:
            consumer["plugins"] = {k: v for plugin in consumer_plugins for k, v in plugin.items()}
        consumers.append(consumer)

    cursor.close()
    conn.close()

    # Export YAML
    config_yaml = {
        "routes": routes,
        "services": services,
        "upstreams": upstreams,
        "consumers": consumers,
    }

    with open("config.yaml", "w") as f:
        yaml.dump(config_yaml, f, sort_keys=False, allow_unicode=True)

    print("✅ 已產出 config.yaml")

if __name__ == "__main__":
    main()