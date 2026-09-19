# Starlight Stays & Resorts

## 1. Project overview and problem statement

Starlight Stays & Resorts is a Java 21 Spring Boot microservices system for multi-property hospitality reservations. It addresses the need to keep room inventory accurate, securely let customers manage their own reservations, and prevent overlapping confirmed bookings for the same room.

## 2. Functional requirements

- Register and authenticate users with JWTs.
- Manage properties and rooms and search room inventory.
- Create, retrieve, list, and cancel bookings.
- Validate dates and guest count, verify a room, and prevent booking conflicts.
- Enforce user booking ownership while permitting administrator operations.

## 3. Five-service architecture

| Service | Port | Responsibility |
| --- | ---: | --- |
| Eureka Server | 8761 | Service discovery registry |
| API Gateway | 8080 | Client entry point and route forwarding |
| User Service | 8081 | Registration, login, JWT issuance, user data |
| Room Service | 8082 | Property, room, search, availability APIs |
| Booking Service | 8083 | Booking lifecycle, ownership, conflicts |

Clients call the Gateway. Services register by application name in Eureka, and Gateway resolves explicit `lb://` routes rather than exposing service hostnames.

## 4. Eureka service discovery and API Gateway

Eureka Server supplies the registry. API Gateway, User Service, Room Service, and Booking Service register and fetch the registry through `EUREKA_DEFAULT_ZONE`, defaulting to `http://localhost:8761/eureka/`. On 19 September 2026, verified registrations were `API-GATEWAY`, `USER-SERVICE`, `ROOM-SERVICE`, and `BOOKING-SERVICE`.

| Gateway path | Target |
| --- | --- |
| `/api/auth/**`, `/api/users/**` | `lb://USER-SERVICE` |
| `/api/properties/**`, `/api/rooms/**` | `lb://ROOM-SERVICE` |
| `/api/bookings/**` | `lb://BOOKING-SERVICE` |

Gateway health was verified at `/actuator/health`. This installed Gateway version did not publish `/actuator/gateway/routes`; actual successful routed requests and Gateway route logs verified routing instead.

## 5. JWT authentication and security

User Service issues signed JWTs with the user ID as subject and email/role claims. Booking Service validates the same configured `JWT_SECRET`, uses the subject as the authenticated owner, and applies `ROLE_USER` and `ROLE_ADMIN` authorities. A USER's owner ID never comes from `BookingRequest.userId`; a USER can list, retrieve, and cancel only their own bookings. ADMIN operations for all bookings are implemented according to the existing role design.

ADMIN authorization is implemented but not manually exercised because no ADMIN credential was available. USER ownership was exercised with two authenticated USER accounts.

## 6. Room inventory, booking lifecycle, and Feign

Room Service manages properties, rooms, types, prices, capacity, active state, searches, and availability. Booking Service creates `CONFIRMED` bookings after validation and supports retrieval, user lists, and cancellation to `CANCELLED`.

Before persistence, Booking Service calls Room Service through the `ROOM-SERVICE` OpenFeign client. A missing room is mapped to the booking API's room-not-found response. Successful live booking creation verified this Feign-backed lookup.

## 7. Double-booking prevention

Booking Service requires check-in before check-out and finds conflicts for the same room where a confirmed booking has `checkIn < requestedCheckOut` and `checkOut > requestedCheckIn`. It throws `BookingConflictException`, mapped to HTTP 409. Cancelled bookings do not block later bookings.

## 8. Integration and security testing results

Only executed checks are listed.

| Check | Result | Evidence |
| --- | --- | --- |
| Gateway health | Passed | HTTP 200 |
| User login | Passed | HTTP 200 for supplied test user |
| JWT-protected user endpoint | Passed | HTTP 200 |
| Room search | Passed | HTTP 200 through Gateway |
| Booking creation | Passed | HTTP 201 through Gateway |
| JWT ownership binding | Passed | Spoofed request user ID replaced by JWT subject |
| Booking retrieval and user list | Passed | Both HTTP 200 |
| Booking cancellation | Passed | HTTP 200 |
| Overlap prevention | Passed | Duplicate confirmed dates returned HTTP 409 |
| Feign Room Service lookup | Passed | Required by successful live create flow |
| Unauthenticated booking request | Passed | Rejected with HTTP 403 |
| Cross-user booking access | Passed | Second USER received HTTP 403 |
| ADMIN authorization | Not manually exercised | No ADMIN credential available |

## 9. Deployment and verification

All five modules were packaged with Maven 3.9.16, Java 21, and `mvn clean package -DskipTests`.

| Service | Executable JAR |
| --- | --- |
| Eureka Server | `eureka-server/target/eureka-server-1.0.0.jar` |
| API Gateway | `api-gateway/target/api-gateway-1.0.0.jar` |
| User Service | `user-service/target/user-service-1.0.0.jar` |
| Room Service | `room-service/target/room-service-0.0.1-SNAPSHOT.jar` |
| Booking Service | `booking-service/target/booking-service-0.0.1-SNAPSHOT.jar` |

Environment overrides preserve verified local defaults: `EUREKA_SERVER_PORT`, `GATEWAY_PORT`, `USER_SERVICE_PORT`, `ROOM_SERVICE_PORT`, `BOOKING_SERVICE_PORT`, `EUREKA_HOSTNAME`, `EUREKA_DEFAULT_ZONE`, `JWT_SECRET`, and the `USER_DB_*`, `ROOM_DB_*`, and `BOOKING_DB_*` database variables.

No Docker deployment was added: no supplied rubric requires it, and executable JAR deployment is complete and verified without introducing an untested second runtime.

### Startup order

1. Start MySQL or provide database environment variables.
2. Start Eureka and wait for `http://localhost:8761/actuator/health`.
3. Start User and Room services and wait for health.
4. Start Booking Service and verify Eureka registration.
5. Start API Gateway and use `http://localhost:8080`.

All five health endpoints returned HTTP 200 on ports 8761, 8080, 8081, 8082, and 8083.

## 10. DTI concepts and innovation

The project demonstrates digital transformation through independently deployable hospitality services, discovery rather than fixed locations, a governed Gateway entry point, stateless JWT security, and automated inventory-integrity controls. This supports future web/mobile clients, independent releases, and selective scaling.

## 11. LinkedIn article draft

**Building Starlight Stays & Resorts: a secure microservices booking platform**

I built a Java 21 Spring Boot microservices platform for multi-property hospitality management using Eureka, Spring Cloud Gateway, OpenFeign, MySQL, Spring Security, and JWT. Identity, room inventory, bookings, routing, and discovery are separated so each concern can evolve independently. Booking Service validates dates, verifies rooms through Feign, and prevents overlapping confirmed reservations with HTTP 409. JWT ownership checks let customers manage only their own reservations. Gateway-level verification covered discovery, routing, login, room lookup, booking creation, conflict handling, cancellation, and cross-user authorization.

## 12. Final rubric mapping

| Rubric area | Status |
| --- | --- |
| Five Spring Boot services | Complete |
| Java 21 build and executable JARs | Complete |
| Eureka discovery | Complete and verified |
| Gateway routing | Complete and verified by live requests/logs |
| JWT authentication | Complete and verified |
| Room inventory/search | Complete; search verified |
| Feign room verification | Complete and verified |
| Booking lifecycle and overlap prevention | Complete and verified |
| USER authorization | Complete and verified |
| ADMIN authorization | Implemented; manual credential test pending |
| Environment-based configuration | Complete with local defaults |
| Docker | Not required by supplied rubric; not added |
