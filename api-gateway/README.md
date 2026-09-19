# API Gateway — PS004 Starlight Stays & Resorts

Single entry point for the **Multi-Property Hospitality Reservation & Management System**.

- **Course:** 24SDCS03R — SOA Programming and Microservices
- **Team:** PS04-S06-02
- **Phase:** 3 of the project (API Gateway)

---

## What this module does

Every client request enters the system here, on port **8080**. The Gateway resolves
the target microservice through **Eureka** using a logical name (`lb://ROOM-SERVICE`)
and forwards the request, preserving the `Authorization` header for the JWT layer
added in Phase 4.

Clients never talk to `:8081`, `:8082` or `:8083` directly.

---

## Technology

| Component | Version |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.15 |
| Spring Cloud | 2025.0.3 (Northfields) |
| Gateway starter | `spring-cloud-starter-gateway-server-webflux` |
| Server | Netty (reactive), not Tomcat |

> The gateway starter was renamed in Spring Cloud 2025.0. `spring-cloud-starter-gateway`
> is deprecated, and the property prefix moved from `spring.cloud.gateway.*` to
> `spring.cloud.gateway.server.webflux.*`.

---

## Routes

| Incoming path | Target service |
|---|---|
| `/api/auth/**` | `lb://USER-SERVICE` |
| `/api/users/**` | `lb://USER-SERVICE` |
| `/api/properties/**` | `lb://ROOM-SERVICE` |
| `/api/rooms/**` | `lb://ROOM-SERVICE` |
| `/api/bookings/**` | `lb://BOOKING-SERVICE` |

---

## Prerequisite

The **Eureka Server must already be running** on `http://localhost:8761`.

---

## Run

```
mvn clean install
mvn spring-boot:run
```

---

## Verify

| Check | URL | Expected |
|---|---|---|
| Gateway health | http://localhost:8080/actuator/health | `{"status":"UP"}` |
| Loaded routes | http://localhost:8080/actuator/gateway/routes | 5 routes with `lb://` URIs |
| Eureka registration | http://localhost:8761 | `API-GATEWAY` listed |

---

## Phase 3 limitation (expected)

`USER-SERVICE`, `ROOM-SERVICE` and `BOOKING-SERVICE` **do not exist yet**.

Calling `/api/users/1`, `/api/rooms` or `/api/bookings` will return a
**503 Service Unavailable**. That is the correct behaviour for this phase — it proves
the route matched and the Gateway tried to resolve the service name through Eureka,
but found no registered instance.

---

## Not implemented in this phase

JWT validation, user/room/booking logic, databases, OpenFeign, circuit breakers.
