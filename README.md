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

## What can this project do?

- Demonstrates how to configure and extend APISIX with custom plugins.
- Provides a minimal service to generate routes dynamically using templates.
- Serves as a starting point for experimenting with APISIX in a local Docker setup.

