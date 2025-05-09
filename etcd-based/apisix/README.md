# APISIX Binding Service

Minimal Spring Boot skeleton that binds user/API/persona information into APISIX via Admin API.

## Tech stack
* Spring Boot 3.2.x (WebFlux + Data JPA)
* MariaDB
* Lombok
* Maven

## Quick start

```bash
# database
mariadb -u root -p < src/main/resources/schema.sql

# build & run
./mvnw spring-boot:run
```

## Generated at

UTC 2025-05-09T15:01:48.117565 
