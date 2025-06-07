# APISIX Route Subscription Service

A minimal Spring Boot service that dynamically subscribes user, API, and personaType information into [Apache APISIX](https://apisix.apache.org/) via the Admin API using template-driven route configuration.

## 🧩 Features

- Route configuration templating (route/plugin/vars)
- Template rendering via `{{...}}` syntax
- Persona-specific `vars` stored in JSON format inside DB
- Pushes to APISIX using Admin API (`/apisix/admin/routes`)
- Lightweight and extensible

## ⚙️ Tech Stack

- **Spring Boot 3.2.x**
  - Spring WebFlux
  - Spring Data JPA
- **MariaDB**
- **Jackson** (for JSON processing)
- **Lombok**
- **Maven**

## Environment Variables

Copy `.env.example` to `.env` and adjust the following variables if needed:

- `APISIX_ADMIN_URL` – base URL of the APISIX Admin API.
- `APISIX_ADMIN_API_KEY` – API key for accessing the Admin API.
