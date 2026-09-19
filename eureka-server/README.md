# Eureka Server — PS004 Starlight Stays & Resorts

Service Discovery Server for the **Multi-Property Hospitality Reservation & Management System**.

- **Course:** 24SDCS03R — SOA Programming and Microservices
- **Team:** PS04-S06-02
- **Phase:** 2 of the project (Service Discovery)

---

## What this module does

This application is the **service registry** for the entire system. Every other
microservice built in later phases will register itself here on startup, and will
look up other services by **logical name** instead of by hardcoded IP/port.

It is a standalone Eureka Server: it does **not** register itself as a client and
does **not** fetch a registry from any peer server.

---

## Technology

| Component | Version |
|---|---|
| Java | 21 (17 also works) |
| Spring Boot | 3.5.15 |
| Spring Cloud | 2025.0.3 (Northfields) |
| Build tool | Maven |

---

## Configuration

| Property | Value |
|---|---|
| `server.port` | 8761 |
| `spring.application.name` | EUREKA-SERVER |
| `eureka.client.register-with-eureka` | false |
| `eureka.client.fetch-registry` | false |

---

## Run

From the `eureka-server` folder:

```bash
mvn clean install
mvn spring-boot:run
```

Then open: **http://localhost:8761**

---

## Expected state in Phase 2

The dashboard will load and show **no registered instances**. This is correct —
no client services exist yet.

Services that will register in later phases:

- `API-GATEWAY` (Phase 3)
- `USER-SERVICE` (Phase 4)
- `ROOM-SERVICE` (Phase 5)
- `BOOKING-SERVICE` (Phase 6)

---

## Health check

```
GET http://localhost:8761/actuator/health
→ {"status":"UP", ...}
```
