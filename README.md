# APISIX Proof of Concept

This repository provides a simple environment for experimenting with [Apache APISIX](https://apisix.apache.org/).
It includes custom plugins, a Spring Boot service for dynamic route subscription and a Docker based setup with MariaDB and etcd.

## Project Structure

- **config/** – configuration files for APISIX, dashboard and monitoring
- **plugins/** – custom APISIX plugins written in Lua
- **service/** – Spring Boot service for managing route subscriptions
- **scripts/** – helper scripts (e.g. export APISIX settings)
- **sql/** – SQL scripts used to initialize the MariaDB instance
- **docker-compose.yaml** – compose file to start APISIX, etcd, MariaDB and the dashboard

## Usage

1. **Prepare configuration and environment variables**
   If `docker-compose up` fails with `permission denied` errors, adjust the
   permissions of the configuration folders so the containers can read them:
   ```bash
   sudo chmod -R 755 config plugins sql scripts
   sudo chmod +x scripts/export-settings.sh
   ```

   Create the `etcd_data` directory for the etcd container:
   ```bash
   mkdir -p etcd_data
   chmod 755 etcd_data
   ```

   Copy `.env.example` to `.env` and verify the contents:
   ```bash
   cp .env.example .env
   cat .env
   ```
   A valid `.env` file looks like this:
   ```dotenv
   APISIX_ADMIN_URL=http://localhost:9180/apisix/admin
   APISIX_ADMIN_API_KEY=admin123
   ```

2. **Start the stack**
   ```bash
   docker-compose up -d
   ```
   This launches APISIX, etcd, MariaDB and the dashboard.

3. **Run the Spring Boot service**
   ```bash
   cd service
   mvn spring-boot:run
   ```
   The service exposes APIs for subscribing routes which are pushed to APISIX via the Admin API.

4. **Export current APISIX settings**
   ```bash
   bash scripts/export-settings.sh
   ```
   The script saves the output JSON files under `config/raw/`.

5. **Run the curl test script**
   The test script sends example subscription requests to the service. If the
   service is not running on `localhost:8080`, override `SERVICE_URL`:
   ```bash
   SERVICE_URL=http://localhost:8080 bash scripts/test-subscribe.sh
   ```
   Each case prints the response and HTTP status code so you can verify success
   or failure.  Examples cover rate limiting and multi-upstream routing.

6. **Reset the environment**
   When you need a clean etcd and database state, run:
   ```bash
   bash scripts/reset-compose.sh
   ```
   The script removes the existing volumes and `etcd_data` directory then
   starts the stack again.

## What can this project do?

- Demonstrates how to configure and extend APISIX with custom plugins.
- Provides a minimal service to generate routes dynamically using templates.
- Serves as a starting point for experimenting with APISIX in a local Docker setup.

## Subscription Logic

The Spring Boot service subscribes routes on demand. Template records are stored
in MariaDB:

1. `route_templates` &ndash; holds JSON snippets for the APISIX route, related
   plugins and persona specific `vars` conditions.
2. `upstream_templates` &ndash; describes upstream objects that can be rendered
   with variables.
3. `api_definitions` &ndash; maps an API name to a pair of templates.

`/apisix/subscribe` accepts a payload with `userName`, `personaType`, `apiKey`,
requested API names and optional `extraParams`. For each API the service:

1. Loads the corresponding templates.
2. Renders the upstream template (one or many if `multi_upstreams` is provided)
   and ensures the upstream exists via the Admin API.
3. Renders `vars_template` and `plugin_template`.
4. Builds a route object, sends it to APISIX and stores a subscription record.

`extraParams.multi_upstreams` allows defining additional upstreams and routing
rules for the `multiple-upstream-plugin`. Each item may specify a `match` value
for the `http_model` header, an optional `api_key` header to inject and custom
upstream settings.

### Notes

- Missing template variables cause validation errors when subscribing.
- The optional `limit-count` plugin requires both `count` and `time_window` in
  `extraParams`.
- Upstream templates that use `{{nodes_json}}` need a non-empty `nodes` array in
  `extraParams`.

## Adding a New API Definition

1. **Create a route template** – insert a row into `route_templates` with a
   unique `code` and provide JSON for `route_template`, `plugin_template` and
   `vars_template`.
2. **Create an upstream template** – insert a row into `upstream_templates` with
   a unique `code` and the upstream JSON.
3. **Register the API** – insert into `api_definitions` with the API name, URI,
   service name and the two template codes.
4. **Restart the service** so the new templates are loaded.

After these steps, clients can include the new API name in the `apis` list when
posting to `/apisix/subscribe`.

