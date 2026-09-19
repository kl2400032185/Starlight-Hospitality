# PS004 — PHASE 3: API GATEWAY IMPLEMENTATION

**Project:** Multi-Property Hospitality Reservation & Management System
**Company:** Starlight Stays & Resorts
**Course:** 24SDCS03R — SOA Programming and Microservices
**Academic Year:** 2026–2027, Odd Semester
**Team:** PS04-S06-02

---

## 0. CRITICAL VERSION FINDING — READ THIS FIRST

**Spring Cloud 2025.0 renamed the Gateway artifacts and moved its property prefix.**
Almost every tutorial you will find online is now wrong for our version.

### Artifact rename

| Deprecated artifact (old tutorials) | Current artifact (what we use) |
|---|---|
| `spring-cloud-starter-gateway` | **`spring-cloud-starter-gateway-server-webflux`** |
| `spring-cloud-gateway-server` | `spring-cloud-gateway-server-webflux` |
| `spring-cloud-starter-gateway-mvc` | `spring-cloud-starter-gateway-server-webmvc` |

Using the deprecated name still works in 2025.0.x but **prints a deprecation warning in the
logs** — and it is removed entirely in Spring Cloud 2025.1 (Oakwood). Since our evaluator may
look at the console, we use the current name.

The new names exist to separate two dimensions: *gateway style* (server vs. proxy-exchange) and
*web stack* (WebFlux vs. WebMVC). We use **server + WebFlux**, which is the reactive,
non-blocking gateway — the standard choice, and the one that runs on **Netty rather than
Tomcat**.

### Property prefix move

| Deprecated prefix | Current prefix |
|---|---|
| `spring.cloud.gateway.*` | **`spring.cloud.gateway.server.webflux.*`** |

This one is dangerous, because a wrong prefix does **not** throw an error — Spring simply
ignores unknown properties. Your routes would silently never load and every request would
404. This is the single most likely mistake in Phase 3.

### Final version set

| Component | Version | Source of compatibility |
|---|---|---|
| Java | 21 | Matches Phase 2 |
| Spring Boot | 3.5.15 | Matches Phase 2 |
| Spring Cloud | 2025.0.3 (Northfields) | Officially based on Spring Boot 3.5.15 |
| Spring Cloud Gateway | 4.3.5 | Supplied by the BOM — never declared manually |
| Spring Cloud Netflix (Eureka client) | 4.3.3 | Supplied by the BOM |

Identical Boot + Cloud versions as `eureka-server`, so the two modules cannot drift apart.

---

## 1. FILES

### FILE PATH: `api-gateway/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.15</version>
        <relativePath/>
    </parent>

    <groupId>com.starlight</groupId>
    <artifactId>api-gateway</artifactId>
    <version>1.0.0</version>
    <name>api-gateway</name>
    <description>PS004 Starlight Stays - API Gateway (single entry point)</description>

    <properties>
        <java.version>21</java.version>
        <spring-cloud.version>2025.0.3</spring-cloud.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>

        <!--
            Spring Cloud Gateway (reactive / WebFlux flavour).

            IMPORTANT: in Spring Cloud 2025.0 the gateway artifacts were renamed.
            The old 'spring-cloud-starter-gateway' is DEPRECATED and logs a warning.
            The current name is 'spring-cloud-starter-gateway-server-webflux'.

            This starter brings in spring-boot-starter-webflux (Netty, NOT Tomcat),
            the route predicates/filters, and the reactive load-balancer client filter.
        -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-gateway-server-webflux</artifactId>
        </dependency>

        <!--
            Registers this Gateway with Eureka as API-GATEWAY and lets it resolve
            logical service names (USER-SERVICE, ROOM-SERVICE, BOOKING-SERVICE).
            It transitively includes spring-cloud-starter-loadbalancer, which is what
            actually makes the lb:// scheme work - so we do NOT declare it separately.
        -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>

        <!-- /actuator/health and /actuator/gateway/routes for verification and demo -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- JUnit 5 + AssertJ + Spring test context, test scope only -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

#### Dependency explanation

| Dependency | Why it is needed |
|---|---|
| `spring-boot-starter-parent` | Manages all Spring Boot library versions and the Boot Maven plugin. |
| `spring-cloud-dependencies` (BOM) | Pins the whole Northfields release train so gateway + Eureka + load balancer are mutually compatible. |
| `spring-cloud-starter-gateway-server-webflux` | **The gateway engine.** Route predicates, filters, and the `ReactiveLoadBalancerClientFilter` that resolves `lb://`. Pulls in `spring-boot-starter-webflux` → Netty. |
| `spring-cloud-starter-netflix-eureka-client` | Registers the Gateway as `API-GATEWAY` and gives it the registry it needs to turn `ROOM-SERVICE` into a real address. **Transitively includes `spring-cloud-starter-loadbalancer`**, which is why we don't declare that separately. |
| `spring-boot-starter-actuator` | `/actuator/health` (demo evidence) and `/actuator/gateway/routes` (proves routes loaded). |
| `spring-boot-starter-test` | JUnit 5 + AssertJ for the Phase 3 tests. Test scope only. |

**Deliberately absent:** `spring-boot-starter-web` (would clash with WebFlux and break the
gateway), `spring-boot-starter-security`, JWT libraries, any database driver, OpenFeign,
Resilience4j.

> **Do not add `spring-boot-starter-web` to this module.** Spring Cloud Gateway is reactive.
> If both starters are present, Boot starts a servlet (Tomcat) web application and the gateway
> auto-configuration backs off, producing the confusing error
> *"Spring MVC found on classpath, which is incompatible with Spring Cloud Gateway"*.

---

### FILE PATH: `api-gateway/src/main/java/com/starlight/gateway/ApiGatewayApplication.java`

```java
package com.starlight.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PS004 - Multi-Property Hospitality Reservation and Management System
 * Starlight Stays and Resorts
 *
 * API Gateway - the single entry point for every client request.
 *
 * Responsibilities:
 *   - Receive all client traffic on port 8080
 *   - Resolve logical service names through Eureka (lb:// routes)
 *   - Forward requests, including the Authorization header, to the target service
 *   - Apply CORS centrally
 *
 * Note on annotations: @EnableDiscoveryClient is NOT used. Since Spring Cloud
 * Edgware, discovery clients are auto-enabled whenever a discovery
 * implementation (here: spring-cloud-starter-netflix-eureka-client) is on the
 * classpath. Adding the annotation would be redundant, not harmful - but this
 * project follows the current recommended style.
 *
 * Registers with Eureka as: API-GATEWAY
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
```

**On `@EnableDiscoveryClient`:** many tutorials still add it. It has been unnecessary for years
— Spring Boot auto-configures the discovery client from the classpath. If your evaluator asks
why it's missing, that's the answer: *"it's redundant since Spring Cloud Edgware; the Eureka
client starter on the classpath is enough."*

---

### FILE PATH: `api-gateway/src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: API-GATEWAY

  cloud:
    gateway:
      # NOTE: Spring Cloud 2025.0 moved the reactive gateway properties from
      # 'spring.cloud.gateway.*' to 'spring.cloud.gateway.server.webflux.*'.
      # Using the old prefix here would silently do nothing.
      server:
        webflux:
          # Explicit routes only. Automatic discovery-based routes are disabled so
          # that the URL contract (/api/**) stays under our control and internal
          # service names are never exposed as public URLs.
          discovery:
            locator:
              enabled: false

          routes:
            # ---------- AUTHENTICATION (User Service) ----------
            # Public endpoints. JWT is issued here in Phase 4.
            - id: auth-service-route
              uri: lb://USER-SERVICE
              predicates:
                - Path=/api/auth/**

            # ---------- USER SERVICE ----------
            - id: user-service-route
              uri: lb://USER-SERVICE
              predicates:
                - Path=/api/users/**

            # ---------- ROOM SERVICE (properties + rooms) ----------
            - id: property-service-route
              uri: lb://ROOM-SERVICE
              predicates:
                - Path=/api/properties/**

            - id: room-service-route
              uri: lb://ROOM-SERVICE
              predicates:
                - Path=/api/rooms/**

            # ---------- BOOKING SERVICE ----------
            - id: booking-service-route
              uri: lb://BOOKING-SERVICE
              predicates:
                - Path=/api/bookings/**

          # Centralised CORS. Configured once here instead of in every microservice.
          globalcors:
            cors-configurations:
              '[/**]':
                # Explicit development origins. NOT '*', because allow-credentials
                # is true and the two must never be combined.
                allowedOrigins:
                  - "http://localhost:3000"
                  - "http://localhost:5173"
                  - "http://localhost:4200"
                allowedMethods:
                  - GET
                  - POST
                  - PUT
                  - DELETE
                  - OPTIONS
                allowedHeaders:
                  - Authorization
                  - Content-Type
                # Lets the browser read these from the response.
                exposedHeaders:
                  - Authorization
                allowCredentials: true
                maxAge: 3600

eureka:
  client:
    register-with-eureka: true
    fetch-registry: true
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
    # Makes the entry on the Eureka dashboard unambiguous when several
    # services run on the same machine.
    instance-id: ${spring.application.name}:${server.port}

management:
  endpoints:
    web:
      exposure:
        # 'gateway' exposes /actuator/gateway/routes - useful demo evidence
        # that routes were loaded, without needing the target services running.
        include: health,info,gateway
  endpoint:
    health:
      show-details: always

logging:
  level:
    root: INFO
    com.starlight.gateway: INFO
    # Shows route matching decisions during development.
    org.springframework.cloud.gateway: INFO
    org.springframework.cloud.gateway.handler.RoutePredicateHandlerMapping: DEBUG
  pattern:
    console: "%d{HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n"
```

**Note the contrast with Phase 2:** the Eureka *server* had `register-with-eureka: false`
because it *is* the registry. The Gateway is a *client*, so both flags are `true` — it must
register itself **and** download the registry to resolve `lb://` names.

---

### FILE PATH: `api-gateway/src/main/java/com/starlight/gateway/filter/RequestLoggingFilter.java`

```java
package com.starlight.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Development-time request/response logging for the Gateway.
 *
 * Logs, for every request:
 *   - the incoming HTTP method and path
 *   - the route id that matched
 *   - the target service URI the request was forwarded to
 *   - the response status and elapsed time
 *
 * Deliberately logs ONLY whether an Authorization header is present - never its
 * value. Tokens, passwords and secrets must never reach the log files.
 */
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private static final String START_TIME_ATTR = "starlight.requestStartTime";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getURI().getPath();
        boolean hasAuthHeader = exchange.getRequest().getHeaders().containsKey("Authorization");

        exchange.getAttributes().put(START_TIME_ATTR, System.currentTimeMillis());

        log.info("--> Incoming request: {} {} | Authorization header present: {}",
                method, path, hasAuthHeader);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {

            Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            String routeId = (route != null) ? route.getId() : "NO_ROUTE_MATCHED";
            String targetUri = (route != null) ? String.valueOf(route.getUri()) : "-";

            HttpStatusCode status = exchange.getResponse().getStatusCode();

            Long startTime = exchange.getAttribute(START_TIME_ATTR);
            long elapsedMs = (startTime != null) ? System.currentTimeMillis() - startTime : -1L;

            log.info("<-- Completed: {} {} | route='{}' | target='{}' | status={} | {}ms",
                    method, path, routeId, targetUri, status, elapsedMs);
        }));
    }

    /**
     * Runs very early in the "pre" phase and very late in the "post" phase,
     * so the logged status is the final status returned to the client.
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
```

This is the class that gives you the required log evidence: **incoming request → matched route
→ target service → response status.** It logs only the *presence* of the Authorization header,
never its value.

---

### FILE PATH: `api-gateway/src/main/java/com/starlight/gateway/error/GatewayErrorAttributes.java`

```java
package com.starlight.gateway.error;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Produces a single, consistent JSON error shape for every error the Gateway
 * itself returns (routing failures, unreachable services, malformed requests).
 *
 * Shape:
 * {
 *   "timestamp": "2026-09-18T14:03:22",
 *   "status": 503,
 *   "error": "Service Unavailable",
 *   "errorCode": "SERVICE_UNAVAILABLE",
 *   "message": "The requested service is not reachable.",
 *   "path": "/api/rooms/1"
 * }
 *
 * This is the same envelope the downstream microservices will adopt in later
 * phases, so clients see one error format no matter where the failure happened.
 *
 * Note: errors produced INSIDE a microservice are passed through untouched -
 * this class only shapes errors that the Gateway generates.
 */
@Component
public class GatewayErrorAttributes extends DefaultErrorAttributes {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {

        Map<String, Object> defaults = super.getErrorAttributes(request, options);

        int status = toInt(defaults.get("status"), 500);
        String reason = String.valueOf(defaults.getOrDefault("error", "Internal Server Error"));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT));
        body.put("status", status);
        body.put("error", reason);
        body.put("errorCode", toErrorCode(status));
        body.put("message", defaultMessageFor(status));
        body.put("path", String.valueOf(defaults.getOrDefault("path", request.path())));

        return body;
    }

    private int toInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return fallback;
    }

    /**
     * Maps an HTTP status to the stable, machine-readable code used across the
     * whole PS004 system.
     */
    private String toErrorCode(int status) {
        return switch (status) {
            case 400 -> "BAD_REQUEST";
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "RESOURCE_NOT_FOUND";
            case 409 -> "CONFLICT";
            case 503 -> "SERVICE_UNAVAILABLE";
            default -> "INTERNAL_SERVER_ERROR";
        };
    }

    /**
     * Generic, non-leaking messages. Internal exception details are never sent
     * to the client, because they can reveal internal hostnames and stack traces.
     */
    private String defaultMessageFor(int status) {
        return switch (status) {
            case 400 -> "The request was malformed or contained invalid parameters.";
            case 401 -> "Authentication is required to access this resource.";
            case 403 -> "You do not have permission to perform this operation.";
            case 404 -> "No route or resource matched the requested path.";
            case 409 -> "The request conflicts with the current state of the resource.";
            case 503 -> "The requested service is currently unavailable. Please try again later.";
            default -> "An unexpected error occurred while processing the request.";
        };
    }
}
```

---

### FILE PATH: `api-gateway/src/test/java/com/starlight/gateway/ApiGatewayApplicationTests.java`

```java
package com.starlight.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.config.GlobalCorsProperties;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 3 tests.
 *
 * These deliberately run WITHOUT a running Eureka Server and WITHOUT
 * USER-SERVICE / ROOM-SERVICE / BOOKING-SERVICE, because none of them exist yet.
 * Eureka registration is switched off for the test context so the build never
 * depends on another process being up.
 *
 * What is actually verified: that the application context starts, that the YAML
 * route definitions bind correctly to real Gateway configuration objects, and
 * that every route targets a logical lb:// name rather than a hardcoded address.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "eureka.client.enabled=false",
                "eureka.client.register-with-eureka=false",
                "eureka.client.fetch-registry=false"
        }
)
class ApiGatewayApplicationTests {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private Environment environment;

    @Autowired
    private GatewayProperties gatewayProperties;

    @Autowired
    private GlobalCorsProperties globalCorsProperties;

    @Test
    @DisplayName("Spring application context loads")
    void contextLoads() {
        assertThat(context).isNotNull();
    }

    @Test
    @DisplayName("Application registers under the logical name API-GATEWAY")
    void applicationNameIsCorrect() {
        assertThat(environment.getProperty("spring.application.name")).isEqualTo("API-GATEWAY");
    }

    @Test
    @DisplayName("All five gateway routes are loaded from configuration")
    void allRoutesAreConfigured() {
        List<RouteDefinition> routes = gatewayProperties.getRoutes();

        assertThat(routes).hasSize(5);
        assertThat(routes)
                .extracting(RouteDefinition::getId)
                .containsExactlyInAnyOrder(
                        "auth-service-route",
                        "user-service-route",
                        "property-service-route",
                        "room-service-route",
                        "booking-service-route");
    }

    @Test
    @DisplayName("Every route targets a logical lb:// service name, never a hardcoded host")
    void allRoutesUseLoadBalancedServiceNames() {
        List<RouteDefinition> routes = gatewayProperties.getRoutes();

        assertThat(routes)
                .allSatisfy(route -> assertThat(route.getUri().getScheme()).isEqualTo("lb"));

        assertThat(routes)
                .extracting(route -> route.getUri().toString())
                .containsOnly(
                        "lb://USER-SERVICE",
                        "lb://ROOM-SERVICE",
                        "lb://BOOKING-SERVICE");
    }

    @Test
    @DisplayName("Routes map the expected paths to the expected services")
    void routePathsMapToCorrectServices() {
        assertThat(uriOf("auth-service-route")).isEqualTo("lb://USER-SERVICE");
        assertThat(uriOf("user-service-route")).isEqualTo("lb://USER-SERVICE");
        assertThat(uriOf("property-service-route")).isEqualTo("lb://ROOM-SERVICE");
        assertThat(uriOf("room-service-route")).isEqualTo("lb://ROOM-SERVICE");
        assertThat(uriOf("booking-service-route")).isEqualTo("lb://BOOKING-SERVICE");

        assertThat(predicateOf("booking-service-route")).contains("/api/bookings/**");
        assertThat(predicateOf("auth-service-route")).contains("/api/auth/**");
    }

    @Test
    @DisplayName("CORS is configured and does not combine wildcard origin with credentials")
    void corsConfigurationIsSafe() {
        Map<String, CorsConfiguration> corsConfigurations =
                globalCorsProperties.getCorsConfigurations();

        assertThat(corsConfigurations).containsKey("/**");

        CorsConfiguration cors = corsConfigurations.get("/**");

        assertThat(cors.getAllowedMethods())
                .containsExactlyInAnyOrder("GET", "POST", "PUT", "DELETE", "OPTIONS");
        assertThat(cors.getAllowedHeaders())
                .containsExactlyInAnyOrder("Authorization", "Content-Type");

        // The security rule: credentials may only be allowed with explicit origins.
        assertThat(cors.getAllowCredentials()).isTrue();
        assertThat(cors.getAllowedOrigins()).isNotNull();
        assertThat(cors.getAllowedOrigins()).doesNotContain("*");
    }

    private String uriOf(String routeId) {
        return findRoute(routeId).getUri().toString();
    }

    private String predicateOf(String routeId) {
        return findRoute(routeId).getPredicates().toString();
    }

    private RouteDefinition findRoute(String routeId) {
        Optional<RouteDefinition> route = gatewayProperties.getRoutes().stream()
                .filter(r -> routeId.equals(r.getId()))
                .findFirst();

        assertThat(route).as("route '%s' should be configured", routeId).isPresent();
        return route.get();
    }
}
```

#### Why these tests are meaningful

| Test | What it would catch |
|---|---|
| `contextLoads` | Wrong starter, `spring-boot-starter-web` clash, version mismatch, bad YAML. |
| `applicationNameIsCorrect` | Typo in the Eureka registration name. |
| `allRoutesAreConfigured` | **The prefix trap.** If someone writes `spring.cloud.gateway.routes` instead of `spring.cloud.gateway.server.webflux.routes`, this list comes back empty and the test fails loudly — instead of you discovering it during the live demo. |
| `allRoutesUseLoadBalancedServiceNames` | Anyone sneaking in a hardcoded `http://localhost:8082`. |
| `routePathsMapToCorrectServices` | `/api/rooms` accidentally pointing at `BOOKING-SERVICE`, etc. |
| `corsConfigurationIsSafe` | The `allowedOrigins: "*"` + `allowCredentials: true` combination, which browsers reject and which is a genuine security flaw. |

Crucially, they need **no Eureka and no downstream services** — Eureka is disabled for the test
context, so `mvn test` works on a clean machine.

---

### FILE PATH: `api-gateway/README.md`

*(Provided as a separate file — see the attached `api-gateway/README.md`.)*

### FILE PATH: `api-gateway/.gitignore`

```gitignore
target/
!.mvn/wrapper/maven-wrapper.jar

### VS Code ###
.vscode/
.factorypath

### IntelliJ / Eclipse ###
.idea/
*.iml
.classpath
.project
.settings/

### OS ###
Thumbs.db
.DS_Store

### Logs ###
*.log
```

---

## 2. PROJECT TREE

```
Starlight-Hospitality/
│
├── eureka-server/                        ← Phase 2 (already done)
│   ├── pom.xml
│   ├── README.md
│   ├── .gitignore
│   └── src/
│       ├── main/
│       │   ├── java/com/starlight/eureka/EurekaServerApplication.java
│       │   └── resources/application.yml
│       └── test/
│           └── java/com/starlight/eureka/EurekaServerApplicationTests.java
│
└── api-gateway/                          ← Phase 3 (this phase)
    ├── pom.xml
    ├── README.md
    ├── .gitignore
    └── src/
        ├── main/
        │   ├── java/
        │   │   └── com/
        │   │       └── starlight/
        │   │           └── gateway/
        │   │               ├── ApiGatewayApplication.java
        │   │               ├── error/
        │   │               │   └── GatewayErrorAttributes.java
        │   │               └── filter/
        │   │                   └── RequestLoggingFilter.java
        │   └── resources/
        │       └── application.yml
        └── test/
            └── java/
                └── com/
                    └── starlight/
                        └── gateway/
                            └── ApiGatewayApplicationTests.java
```

Only two extra classes beyond the main application were added, and each satisfies an explicit
Phase 3 requirement (logging; centralised error structure). No controllers, no services, no
configuration classes that duplicate what YAML already does.

---

## 3. BUILD COMMANDS

Open PowerShell in VS Code (`Ctrl + ~`).

```powershell
cd "C:\Users\tsusm\OneDrive\Desktop\Starlight-Hospitality\api-gateway"
```

**If `mvn` is on your PATH:**

```powershell
mvn clean install
```

**If PowerShell says `mvn is not recognized`, use the full path:**

```powershell
& "C:\Program Files\Apache\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd" clean install
```

Expected ending: `BUILD SUCCESS`.

> Tip: to avoid retyping that path, set it once per session:
> ```powershell
> $mvn = "C:\Program Files\Apache\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd"
> & $mvn clean install
> ```

---

## 4. TEST COMMANDS

```powershell
mvn test
```

or

```powershell
& "C:\Program Files\Apache\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd" test
```

Expected: `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0` → `BUILD SUCCESS`.

**Eureka does not need to be running for tests.**

---

## 5. RUN COMMANDS — BOTH SERVICES

The Gateway cannot resolve `lb://` names without the registry, so **Eureka must be running
first**. Use two separate terminals and leave both open.

### Terminal 1 — Eureka Server (start this first)

```powershell
cd "C:\Users\tsusm\OneDrive\Desktop\Starlight-Hospitality\eureka-server"
mvn spring-boot:run
```

Full-path version:

```powershell
cd "C:\Users\tsusm\OneDrive\Desktop\Starlight-Hospitality\eureka-server"
& "C:\Program Files\Apache\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd" spring-boot:run
```

Wait until you see `Started EurekaServerApplication`, then confirm http://localhost:8761 loads.

### Terminal 2 — API Gateway

In VS Code: `Terminal → New Terminal` (this opens a *second* terminal; do not reuse Terminal 1,
which is busy running Eureka).

```powershell
cd "C:\Users\tsusm\OneDrive\Desktop\Starlight-Hospitality\api-gateway"
mvn spring-boot:run
```

Full-path version:

```powershell
cd "C:\Users\tsusm\OneDrive\Desktop\Starlight-Hospitality\api-gateway"
& "C:\Program Files\Apache\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd" spring-boot:run
```

Wait for `Started ApiGatewayApplication`.

### Stop

`Ctrl + C` in each terminal.

### Expected endpoints

| Service | URL |
|---|---|
| Eureka dashboard | http://localhost:8761 |
| Gateway health | http://localhost:8080/actuator/health |
| Gateway routes | http://localhost:8080/actuator/gateway/routes |

---

## 6. VERIFICATION

### Check 1 — Gateway starts on 8080

Terminal 2 should show:

```
Netty started on port 8080
Started ApiGatewayApplication in 7.2 seconds
```

Note it says **Netty**, not Tomcat. That confirms the reactive WebFlux gateway started
correctly. If it says Tomcat, `spring-boot-starter-web` has leaked onto the classpath.

### Check 2 — Actuator health

```
http://localhost:8080/actuator/health
```

Expected:

```json
{"status":"UP","components":{"discoveryComposite":{"status":"UP"},"diskSpace":{"status":"UP"},"ping":{"status":"UP"},"refreshScope":{"status":"UP"}}}
```

The `discoveryComposite` component being `UP` is extra evidence that the Eureka client inside
the Gateway connected successfully.

### Check 3 — Eureka dashboard shows API-GATEWAY

```
http://localhost:8761
```

Under **Instances currently registered with Eureka** you should now see:

| Application | AMIs | Availability Zones | Status |
|---|---|---|---|
| API-GATEWAY | n/a (1) | (1) | UP (1) — API-GATEWAY:8080 |

Registration takes up to ~30 seconds after the Gateway starts. If the table is still empty,
wait and refresh before assuming something is broken.

**This single screen is the strongest Phase 3 evidence** — it proves the registry from Phase 2
and the Gateway from Phase 3 are genuinely talking to each other.

### Check 4 — Routes are loaded

```
http://localhost:8080/actuator/gateway/routes
```

Expected: a JSON array of five entries, each showing `"uri": "lb://USER-SERVICE"` (or
ROOM/BOOKING) and its `Path` predicate. This proves the route configuration bound correctly
**without needing any downstream service to exist** — very useful for the demo.

### Check 5 — Route forwarding (deferred, and this is expected)

`USER-SERVICE`, `ROOM-SERVICE` and `BOOKING-SERVICE` **do not exist yet.**

If you call:

```
http://localhost:8080/api/rooms
```

you will get **503 Service Unavailable**, with a body shaped by `GatewayErrorAttributes`.

**This is the correct Phase 3 result, not a bug.** It actually proves three things at once:

1. The Gateway accepted the request on 8080 (single entry point works).
2. The `Path=/api/rooms/**` predicate matched (routing works).
3. The Gateway asked Eureka for `ROOM-SERVICE` and found no registered instance (discovery
   works — it correctly reported that nothing is there).

Your terminal log will show `route='room-service-route' | target='lb://ROOM-SERVICE' |
status=503`, which is exactly the evidence you want.

Full end-to-end forwarding with real business responses is verified in Phases 4–6.

> **Do not write in your report that `/api/users`, `/api/rooms` or `/api/bookings` return
> successful responses at this stage.** They cannot, and claiming so is the kind of thing an
> evaluator will test live.

---

## 7. SCREENSHOT CHECKLIST

Save all of these to `docs/screenshots/`.

| # | Filename | What to capture | Rubric requirement demonstrated |
|---|---|---|---|
| 1 | `phase3_project_structure.png` | VS Code Explorer with the `api-gateway` tree fully expanded, showing `pom.xml`, `ApiGatewayApplication.java`, `application.yml`, the filter and error packages, and the test class | **Rubric 4** — clean, professional Gateway module with clear service separation |
| 2 | `phase3_gateway_started.png` | Terminal 2 showing `Netty started on port 8080` and `Started ApiGatewayApplication` | **Rubric 4** — Gateway is the centralized entry point, running on the architected port |
| 3 | `phase3_actuator_health.png` | Browser at `http://localhost:8080/actuator/health` showing `"status":"UP"` | **Rubric 4** — Gateway operational and observable; supports operational visibility |
| 4 | `phase3_eureka_api_gateway_registered.png` | Browser at `http://localhost:8761` with `API-GATEWAY` visible in the instances table | **Rubric 2 + 4** — dynamic service discovery working end-to-end; the highest-value screenshot of this phase |
| 5 | `phase3_gateway_routes.png` | Browser at `http://localhost:8080/actuator/gateway/routes` showing the five `lb://` routes | **Rubric 4** — dynamic, load-balanced routing configured without hardcoded addresses |
| 6 | `phase3_both_terminals.png` | VS Code with both terminals side by side: Eureka on 8761 and Gateway on 8080 | **Rubric 2** — multi-service architecture running as independent processes |
| 7 (optional) | `phase3_503_expected.png` | Postman calling `GET http://localhost:8080/api/rooms` returning 503, next to the terminal log line showing `route='room-service-route'` | **Rubric 4** — honest evidence that routing and discovery work even though the target service is not yet built |

Screenshot 7 is worth including with a caption explaining *why* 503 is correct. Evaluators
respond well to a team that understands its own error states rather than hiding them.

---

## 8. RUBRIC MAPPING

### Rubric 4 — API Gateway Configuration (primary)

| Requirement | How Phase 3 demonstrates it | Evidence |
|---|---|---|
| **Centralized entry point** | Every client path (`/api/auth`, `/api/users`, `/api/properties`, `/api/rooms`, `/api/bookings`) is served from port 8080. Clients never learn about 8081/8082/8083. | Screenshots 2, 5; Postman collection using only `localhost:8080` |
| **Dynamic routing** | Five `Path` predicates map URL families to services; routes are resolved per-request, not fixed at build time. | `/actuator/gateway/routes`, test `allRoutesAreConfigured` |
| **Eureka service discovery** | Gateway registers as `API-GATEWAY` and fetches the registry to resolve service names. | Screenshot 4 — `API-GATEWAY` on the Eureka dashboard |
| **Load balancing** | Every route uses `lb://`, which routes through `ReactiveLoadBalancerClientFilter`. When a service has multiple instances, requests are distributed automatically. | Test `allRoutesUseLoadBalancedServiceNames`; route list showing `lb://` scheme |
| **CORS** | Configured once centrally with explicit origins, the five required methods, and `Authorization` + `Content-Type` headers. | `application.yml` `globalcors` block; test `corsConfigurationIsSafe` |
| **Security-ready architecture** | Authorization header is preserved and never logged; the Gateway is the designed location for the JWT filter added in Phase 4; no secrets anywhere in config. | `RequestLoggingFilter` logging only header *presence*; Section 9 documentation |
| **Clean service separation** | Gateway contains zero business logic — no controllers, no entities, no repositories. It only routes. | Project tree (Screenshot 1) |

### Rubric 2 — Microservice Identification and Service Discovery (supporting)

Phase 2 built the registry but nothing had registered with it — the dashboard was empty. Phase 3
is the point where service discovery becomes **demonstrable**:

- A second independent process now joins the system and is discovered dynamically.
- `lb://ROOM-SERVICE` in configuration, resolved at runtime, is concrete proof that the
  architecture is discovery-driven rather than address-driven.
- There is **not one hardcoded IP or port** of a backend service anywhere in the Gateway. The
  only hardcoded address is the Eureka URL itself, which is the single, intentional bootstrap
  point every discovery architecture needs.
- The empty-registry state of Phase 2 and the populated state of Phase 3 form a clean
  before/after story for the evaluator.

For a **maximum-marks demonstration** in the final review: start Eureka, show the empty
dashboard, start the Gateway, refresh, and show `API-GATEWAY` appear live. Then stop the
Gateway, refresh again, and show it evicted. That live demo of registration and eviction is
much stronger evidence than any static screenshot.

---

## 9. SECURITY DOCUMENTATION

### Why the Gateway is a single controlled entry point

With one door into the system, security controls are written once instead of five times.
Authentication, rate limiting, CORS and audit logging all live at the boundary. Five separately
exposed services would mean five places to get security right — and five places to get it wrong.

### Why Authorization headers must be protected

The JWT in that header **is** the user's identity for the whole session. Anyone who obtains it
can impersonate the user until it expires. Therefore:

- It is forwarded but **never logged** — `RequestLoggingFilter` logs only whether the header
  exists.
- It must travel over HTTPS in production; HTTP is acceptable only on localhost during
  development.
- It is never placed in a URL query parameter, because URLs land in browser history, proxy logs
  and server access logs.

### Why secrets must not be hardcoded

A JWT signing secret committed to source control is compromised permanently — rotating it means
invalidating every issued token. In Phase 4 the secret will be supplied via an environment
variable or external configuration, never written into `application.yml` in the repository.
Phase 3 contains **no secrets at all**, which is why there is nothing to protect here yet.

### Why CORS must be restricted

CORS is what stops `evil-site.com`, open in another browser tab, from making authenticated
requests to your API using the victim's cookies or tokens. A wildcard origin removes that
protection entirely.

Note the specific rule we follow: **`allowedOrigins: "*"` together with `allowCredentials:
true` is forbidden.** Browsers reject that combination outright, and for good reason — it would
let any website on the internet make credentialed requests to our API. That is exactly why the
config lists three explicit `http://localhost:*` development origins. For production, replace
them with the real frontend domain — one line, one place.

### Why future JWT validation belongs at the security boundary

Validating the token once at the Gateway means an unauthenticated request never reaches a
backend service at all, which reduces both attack surface and wasted load. Downstream services
still perform their own validation (defence in depth, and they may be called service-to-service),
but the Gateway is the first and cheapest place to reject bad traffic.

### Why backend services should not be directly exposed in production

If `ROOM-SERVICE` is reachable on its own public port, an attacker can bypass every Gateway
control — CORS, rate limits, JWT validation — and hit it directly. In production the backend
services bind to a private network and **only the Gateway is publicly routable**. On a laptop
they are all on localhost, which is fine for development, but the report should state the
production intent clearly.

---

## 10. ERROR HANDLING

The Gateway shapes errors through `GatewayErrorAttributes`, producing one consistent envelope.
Handling by status:

| Status | Code | Who produces it in our architecture |
|---|---|---|
| **400** Bad Request | `BAD_REQUEST` | Mostly downstream (validation in Phases 4–6). The Gateway produces it for malformed requests. |
| **401** Unauthorized | `UNAUTHORIZED` | **Phase 4.** Missing/invalid/expired JWT. Will be rejected at the Gateway filter. |
| **403** Forbidden | `FORBIDDEN` | **Phase 4.** Valid token, insufficient role (e.g. USER calling an ADMIN endpoint). |
| **404** Not Found | `RESOURCE_NOT_FOUND` | Gateway: no route predicate matched. Downstream: entity id doesn't exist. |
| **409** Conflict | `CONFLICT` | Downstream only — `ROOM_UNAVAILABLE`, `BOOKING_ALREADY_CANCELLED` (Phase 6). Passed through untouched. |
| **500** Internal Server Error | `INTERNAL_SERVER_ERROR` | Unexpected failures. The client gets a generic message; the stack trace stays in the logs. |
| **503** Service Unavailable | `SERVICE_UNAVAILABLE` | **The one you will actually see in Phase 3.** The route matched but Eureka has no live instance of the target service. |

Kept deliberately minimal — just the envelope structure the later phases will populate. No
circuit breakers or fallback controllers yet; those arrive with the resilience work.

---

## 11. VIVA QUESTIONS

**1. What is an API Gateway?**
It's a single server that sits in front of all the microservices. Every client request goes to
it first, and it forwards each request to the right service.

**2. Why do we need an API Gateway?**
So clients have one address instead of five, and so cross-cutting concerns like security, CORS
and logging are handled in one place instead of being duplicated in every service.

**3. Why use Eureka together with the Gateway?**
Because the Gateway needs to know where the services actually are. Instead of hardcoding IPs
and ports, it asks Eureka for a service by name and gets the current live address.

**4. What does `lb://` mean?**
It stands for load-balanced. `lb://ROOM-SERVICE` tells the Gateway to look up `ROOM-SERVICE` in
Eureka and pick one of its live instances. If there are several, it balances between them.

**5. Why is port 8080 used?**
It's the standard HTTP application port and was fixed in our Phase 1 architecture as the public
entry point. The backend services use 8081, 8082 and 8083, which stay internal.

**6. What is dynamic routing?**
Routes point to service *names* rather than fixed addresses, so the actual target is resolved
fresh at request time. If a service moves or scales up, routing adapts with no config change.

**7. Why should backend services not be directly exposed?**
Because calling them directly would bypass all the Gateway's security — CORS, rate limiting,
and the JWT validation we add in Phase 4.

**8. What is CORS?**
A browser security rule that blocks a web page from calling an API on a different origin unless
that API explicitly allows it. We configure the allowed origins, methods and headers at the
Gateway.

**9. Why preserve Authorization headers?**
Because the JWT in that header identifies the user. If the Gateway stripped it, the downstream
service would have no idea who is making the request and would reject it.

**10. What happens if a target service is unavailable?**
Eureka has no live instance for that name, so the Gateway returns 503 Service Unavailable. That
is exactly what happens in Phase 3, since the User, Room and Booking services don't exist yet.

**Bonus — Why doesn't your Gateway use `@EnableDiscoveryClient`?**
It's unnecessary in modern Spring Cloud. Having the Eureka client starter on the classpath
auto-enables discovery; the annotation has been redundant since Spring Cloud Edgware.

---

## 12. DTI CONNECTION

Using the project's own terminology from Phase 1:

| DTI Concept | Where it appears in Phase 3 | Business benefit |
|---|---|---|
| **Cloud-native architecture** | A stateless, independently deployable Gateway on reactive Netty, with no local state to lose. | Can be containerised and replicated without redesign. |
| **Microservices** | The Gateway holds zero business logic — it only routes. Reservation, inventory and identity logic stay in their own services. | Each capability evolves on its own schedule. |
| **API-first design** | The public URL contract (`/api/auth`, `/api/users`, `/api/properties`, `/api/rooms`, `/api/bookings`) is defined and running **before** a single service behind it exists. | Frontend, mobile and partner integrations can be built in parallel against a stable contract. |
| **Automation** | Routing targets resolve automatically through the registry — no manual address configuration when services restart or scale. | Removes a whole class of manual deployment errors. |
| **Centralized access** | One controlled door into the system for all guest and admin traffic. | Security policy and monitoring applied once, consistently. |
| **Scalability** | `lb://` routing distributes traffic across all registered instances of a service. | Room Service can be scaled up for peak-season search load independently of Booking Service. |
| **Service discovery** | The Gateway registers as `API-GATEWAY` and resolves `ROOM-SERVICE` dynamically through Eureka. | New properties, services or instances are added without reconfiguring existing components. |
| **Resilience** | A dead service produces a clean 503 with a structured error body rather than a hanging request or a leaked stack trace. The architecture has a defined place to add circuit breakers later. | Partial failures stay contained instead of cascading. |
| **Security** | Authorization header preserved but never logged; restricted CORS; the designated place for JWT validation in Phase 4; no secrets in configuration. | Guest data and pricing information protected at the boundary. |
| **Operational visibility** | `/actuator/health`, `/actuator/gateway/routes`, and per-request logging of route, target service, status and latency. | Operations staff can see traffic patterns and diagnose failures without reading code. |

The honest framing for your LinkedIn article: Phase 3 is where the hospitality platform stops
being a set of planned components and becomes an **addressable system**. A guest-facing app can
now be pointed at one URL, and that URL will keep working as services are added, moved and
scaled behind it.

---

## 13. TROUBLESHOOTING

| Problem | Cause | Fix |
|---|---|---|
| Routes never match; everything 404s | Old property prefix `spring.cloud.gateway.routes` | Use `spring.cloud.gateway.server.webflux.routes`. Run `mvn test` — `allRoutesAreConfigured` catches this. |
| `Spring MVC found on classpath, which is incompatible with Spring Cloud Gateway` | `spring-boot-starter-web` present | Remove it. The gateway starter provides WebFlux. |
| Console says `Tomcat started` instead of `Netty started` | Same as above | Same fix. |
| Deprecation warning about gateway artifacts | Using `spring-cloud-starter-gateway` | Switch to `spring-cloud-starter-gateway-server-webflux`. |
| `API-GATEWAY` never appears on the Eureka dashboard | Eureka not running, wrong `defaultZone`, or you didn't wait | Start Eureka first; confirm the URL is `http://localhost:8761/eureka/` (trailing slash); wait ~30s and refresh. |
| Port 8080 already in use | Another app (often a previous run) | `netstat -ano \| findstr :8080` then `taskkill /PID <pid> /F` |
| `mvn` not recognized in PowerShell | Maven not on PATH for this session | Use the full `& "C:\Program Files\Apache\...\mvn.cmd"` form. |
| Browser CORS error despite config | Origin not in the allowed list | Add your frontend's exact origin including port. Do **not** switch to `"*"` while `allowCredentials: true`. |
| 503 on `/api/rooms` | ROOM-SERVICE doesn't exist yet | **Expected in Phase 3.** Not a bug. |

---

## 14. FINAL CONSISTENCY CHECK

| Item | Status |
|---|---|
| Java 21 | ✅ `<java.version>21</java.version>` |
| Spring Boot 3.5.15 | ✅ parent version |
| Spring Cloud 2025.0.3 | ✅ BOM import |
| Maven project | ✅ `com.starlight:api-gateway:1.0.0` |
| Port 8080 | ✅ `server.port: 8080` |
| Application name API-GATEWAY | ✅ `spring.application.name: API-GATEWAY` |
| Eureka URL correct | ✅ `http://localhost:8761/eureka/` |
| USER-SERVICE route | ✅ `/api/users/**` → `lb://USER-SERVICE` |
| ROOM-SERVICE route | ✅ `/api/properties/**` and `/api/rooms/**` → `lb://ROOM-SERVICE` |
| BOOKING-SERVICE route | ✅ `/api/bookings/**` → `lb://BOOKING-SERVICE` |
| AUTH route | ✅ `/api/auth/**` → `lb://USER-SERVICE` |
| `lb://` dynamic routing | ✅ All five routes; enforced by a test |
| CORS | ✅ 5 methods, 2 headers, explicit origins, no wildcard-with-credentials |
| Authorization header preservation | ✅ Forwarded by default; no filter strips it; never logged |
| Actuator health | ✅ `/actuator/health` + `/actuator/gateway/routes` |
| Tests | ✅ 6 tests, no Eureka or downstream services required |
| Current (non-deprecated) gateway artifact | ✅ `spring-cloud-starter-gateway-server-webflux` |
| Current (non-deprecated) property prefix | ✅ `spring.cloud.gateway.server.webflux.*` |
| No User Service implementation | ✅ |
| No Room Service implementation | ✅ |
| No Booking Service implementation | ✅ |
| No JWT implementation | ✅ No security starter, no JWT library, no token code |
| No database | ✅ No JPA, no driver, no entities |
| No Feign client | ✅ |
| No hardcoded backend IP addresses | ✅ Only the Eureka bootstrap URL, which is intentional |

### One honest note on the build environment

These files were authored and structured here, but **the Maven build could not be executed in
this environment** — no Maven binary and no access to Maven Central from the sandbox. Both the
renamed gateway artifact and the new property prefix were verified against Spring's official
2025.0 release notes rather than assumed from memory. Run `mvn clean install` on your Windows
machine as the real compile check; Section 13 covers the failure modes you're most likely to
hit.

---

**PHASE 3 COMPLETE — READY FOR PHASE 4: USER SERVICE + JWT AUTHENTICATION**
