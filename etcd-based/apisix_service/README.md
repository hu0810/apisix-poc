# APISIX Route Binding Service

A minimal Spring Boot service that dynamically binds user, API, and personaType information into [Apache APISIX](https://apisix.apache.org/) via the Admin API using template-driven route configuration.

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