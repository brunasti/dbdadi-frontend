# dbdadi-frontend

Vaadin 24 frontend for the [dbdadi](https://github.com/brunasti/db-data-dictionary) DB Data Dictionary REST API.

## Tech Stack

- Java 21
- Spring Boot 3.4
- Vaadin 24.5
- Maven

## Prerequisites

- Java 21+
- Maven 3.9+
- Node.js 18+ (for Vaadin frontend build — auto-downloaded by Maven if missing)
- The `dbdadi` backend running on port 8080

## Getting Started

1. Start the backend first:
```bash
cd ../data-dictionary
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

2. Start the frontend:
```bash
mvn spring-boot:run
```

3. Open your browser at: **http://localhost:8081**

## Configuration

The backend URL is configured in `application.properties`:

```properties
dbdadi.api.base-url=http://localhost:8080
```

Change it if your backend runs on a different host or port.

## Views

| View | URL | Description |
|---|---|---|
| Dashboard | `/` | Overview with counts |
| Database Models | `/database-models` | CRUD for database models |
| Tables | `/tables` | CRUD for tables, filter by model |
| Columns | `/columns` | CRUD for columns, filter by table |
| Relationships | `/relationships` | CRUD for table relationships |

## Backend API

API docs available at: http://localhost:8080/swagger-ui.html
