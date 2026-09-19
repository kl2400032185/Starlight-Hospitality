# PS004 — PHASE 2: EUREKA SERVER IMPLEMENTATION

**Project:** Multi-Property Hospitality Reservation & Management System
**Company:** Starlight Stays & Resorts
**Course:** 24SDCS03R — SOA Programming and Microservices
**Team:** PS04-S06-02

---

## 1. VERSION COMPATIBILITY

### Chosen combination

| Component | Version | Why |
|---|---|---|
| **Java** | **21** (LTS) | Spring Boot 3.5.x requires Java 17 minimum and fully supports 21. Java 21 is the current LTS and is what VS Code's Java extension pack installs by default. Java 17 also works — change `<java.version>` to `17` if that's what you have. |
| **Spring Boot** | **3.5.15** | Latest maintained 3.5.x line. Uses Jakarta EE 10 namespaces and Spring Framework 6.2.x. |
| **Spring Cloud** | **2025.0.3** (Northfields) | This is the release train that Spring itself declares as **based on Spring Boot 3.5.15**. |
| **Spring Cloud Netflix (Eureka)** | 4.3.3 | Pulled in automatically by the `spring-cloud-dependencies` BOM — you never specify this version yourself. |
| **Maven** | 3.9.x | Standard build tool; anything 3.6.3+ works. |

### Why these are compatible (and why mixing is dangerous)

Spring Cloud is not released as a single library — it is a **release train**, a curated
set of module versions (`spring-cloud-netflix`, `spring-cloud-gateway`,
`spring-cloud-openfeign`, …) that have been tested together against **one specific
Spring Boot line**.

- Spring Cloud **2025.0.x (Northfields)** → Spring Boot **3.5.x**
- Spring Cloud **2024.0.x (Moorgate)** → Spring Boot **3.4.x**
- Spring Cloud **2025.1.x (Oakwood)** → Spring Boot **4.0.x**

Specifically, Spring Cloud 2025.0.3 is documented as being based on Spring Boot 3.5.15,
which is exactly the parent version used in our `pom.xml`. That is the pairing we use.

If you mix trains (for example Spring Boot 3.5.x with Spring Cloud 2024.0.x), Spring Cloud's
built-in **compatibility verifier** fails the application at startup with a message like:

```
Spring Cloud Version X is not compatible with Spring Boot Version Y
```

…or, worse, you get runtime `NoSuchMethodError` / `ClassNotFoundException` from mismatched
`spring-cloud-commons` classes.

### Compatibility rules we follow in this project

1. Declare Spring Boot version **once**, in `<parent>`.
2. Declare Spring Cloud version **once**, as the `spring-cloud-dependencies` BOM import.
3. Never put an explicit `<version>` on any `spring-cloud-*` dependency — let the BOM decide.
4. Use the same Boot + Cloud versions in **every** module of the project (Phases 2–6), so
   Gateway, User, Room and Booking services stay consistent with this Eureka Server.

> Note: Spring Boot 4.0 / Spring Cloud 2025.1 (Oakwood) also exist, but the brief specifies
> Spring Boot 3.x, and 3.5.x + 2025.0.x is the most widely documented, tutorial-supported
> combination for an academic project. Sticking to it avoids surprises during evaluation.

---

## 2. ARCHITECTURE ROLE OF EUREKA (from Phase 1)

Phase 1 defined five components with fixed ports:

| Component | Eureka name | Port | Phase |
|---|---|---|---|
| Eureka Server | EUREKA-SERVER | 8761 | **Phase 2 (this one)** |
| API Gateway | API-GATEWAY | 8080 | Phase 3 |
| User Service | USER-SERVICE | 8081 | Phase 4 |
| Room Service | ROOM-SERVICE | 8082 | Phase 5 |
| Booking Service | BOOKING-SERVICE | 8083 | Phase 6 |

Eureka sits at the centre of this. Two things depend on it:

1. **API Gateway routing.** Phase 1 specifies routes like `lb://ROOM-SERVICE` rather than
   `http://localhost:8082`. The `lb://` scheme only works if the Gateway can resolve
   `ROOM-SERVICE` through a registry — that registry is this Eureka Server.
2. **Booking Service → Room Service (OpenFeign).** Phase 1 specifies that Booking Service
   asks Room Service *"Is Room 101 available from 2026-12-20 to 2026-12-23?"* through a Feign
   client targeting the name `ROOM-SERVICE`. Again, the name is resolved via Eureka.

Because both of these depend on the registry existing first, Eureka Server is built **before**
everything else. It is the foundation the rest of the system plugs into.

---

## 3. COMPLETE FOLDER STRUCTURE

```
eureka-server/
├── pom.xml
├── README.md
├── .gitignore
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── starlight/
    │   │           └── eureka/
    │   │               └── EurekaServerApplication.java
    │   └── resources/
    │       └── application.yml
    └── test/
        └── java/
            └── com/
                └── starlight/
                    └── eureka/
                        └── EurekaServerApplicationTests.java
```

**Package choice:** `com.starlight.eureka`. This keeps the company namespace (`com.starlight`)
consistent across every phase — later modules will be `com.starlight.user`,
`com.starlight.room`, `com.starlight.booking`, `com.starlight.gateway`.

No `controller`, `service`, `entity` or `repository` packages exist here, and that is correct:
a Eureka Server has **no business logic of its own**. All of its behaviour comes from the
Spring Cloud Netflix auto-configuration that `@EnableEurekaServer` switches on.

---

## 4. FILE: eureka-server/pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <!-- Spring Boot parent: manages plugin config and all Spring Boot dependency versions -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.15</version>
        <relativePath/>
    </parent>

    <groupId>com.starlight</groupId>
    <artifactId>eureka-server</artifactId>
    <version>1.0.0</version>
    <name>eureka-server</name>
    <description>PS004 Starlight Stays - Eureka Service Discovery Server</description>

    <properties>
        <java.version>21</java.version>
        <!-- Spring Cloud release train that is officially based on Spring Boot 3.5.15 -->
        <spring-cloud.version>2025.0.3</spring-cloud.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Spring Cloud BOM: aligns every spring-cloud-* artifact to one compatible set -->
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
        <!-- The only functional dependency needed: turns this app into a Eureka registry server.
             It transitively brings in spring-boot-starter-web (embedded Tomcat),
             eureka-core, eureka-client and the Freemarker-based Eureka dashboard UI. -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
        </dependency>

        <!-- Exposes /actuator/health so we can verify the server is UP programmatically -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- JUnit 5 + Spring test support, test scope only -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Enables "mvn spring-boot:run" and builds an executable fat JAR -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

### Dependency explanation

| Dependency | Why it is here |
|---|---|
| `spring-boot-starter-parent` (parent) | Supplies managed versions for every Spring Boot library plus sensible plugin defaults. Removes the need to version anything manually. |
| `spring-cloud-dependencies` (BOM) | Imports the Northfields release train so all `spring-cloud-*` modules stay mutually compatible. |
| `spring-cloud-starter-netflix-eureka-server` | **The core dependency.** Provides the Eureka registry, the REST registration API at `/eureka/**`, and the HTML dashboard at `/`. Pulls in `spring-boot-starter-web` (embedded Tomcat) transitively, which is why we don't declare web separately. |
| `spring-boot-starter-actuator` | Adds `/actuator/health` — used in Section 11 to prove the server is UP without relying only on the browser. |
| `spring-boot-starter-test` | JUnit 5, AssertJ and Spring test context support for Section 8's tests. Test scope only, not shipped in the JAR. |

**Deliberately NOT included** (they belong to later phases): JWT libraries, MySQL driver,
Spring Data JPA, OpenFeign, Spring Cloud Gateway, Spring Security.

---

## 5. FILE: eureka-server/src/main/java/com/starlight/eureka/EurekaServerApplication.java

```java
package com.starlight.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * PS004 - Multi-Property Hospitality Reservation and Management System
 * Starlight Stays and Resorts
 *
 * Eureka Service Discovery Server.
 *
 * This application is the service registry for the whole system. In later phases
 * API-GATEWAY, USER-SERVICE, ROOM-SERVICE and BOOKING-SERVICE will register
 * themselves here, so that they can locate each other by logical service name
 * instead of by hardcoded host and port.
 *
 * Runs on port 8761. Dashboard: http://localhost:8761
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

**The two annotations:**

- `@SpringBootApplication` — standard Spring Boot bootstrap (component scanning +
  auto-configuration + configuration class).
- `@EnableEurekaServer` — imports `EurekaServerMarkerConfiguration`, which registers a marker
  bean. Spring Cloud's `EurekaServerAutoConfiguration` is conditional on that marker bean being
  present. Without this annotation the app would start as a plain web application with no
  registry and no dashboard.

---

## 6. FILE: eureka-server/src/main/resources/application.yml

```yaml
server:
  port: 8761

spring:
  application:
    name: EUREKA-SERVER

eureka:
  instance:
    hostname: localhost
  client:
    # This application IS the registry, so it must not register itself as a client.
    register-with-eureka: false
    # There is no peer/upstream Eureka server, so there is no registry to fetch.
    fetch-registry: false
    service-url:
      defaultZone: http://${eureka.instance.hostname}:${server.port}/eureka/
  server:
    # Development-only setting.
    # Self-preservation stops Eureka from evicting instances when heartbeats drop,
    # which is useful in production but confusing on a laptop: a service you stopped
    # would keep showing as UP. Disabling it makes the dashboard reflect reality quickly.
    enable-self-preservation: false
    # How often (ms) Eureka removes instances that stopped sending heartbeats.
    eviction-interval-timer-in-ms: 15000

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: always

logging:
  level:
    com.netflix.eureka: INFO
    com.netflix.discovery: INFO
```

### Property-by-property

| Property | Purpose |
|---|---|
| `server.port: 8761` | The Eureka convention, and the port fixed by Phase 1. |
| `spring.application.name: EUREKA-SERVER` | The logical name of this application, as required by the brief. |
| `eureka.instance.hostname: localhost` | Tells Eureka what host name to advertise. On a dev machine this avoids odd machine names appearing in the dashboard. |
| `eureka.client.register-with-eureka: false` | **Critical.** Stops the server registering itself in its own registry, which would otherwise pollute the dashboard and cause noisy connection retries. |
| `eureka.client.fetch-registry: false` | **Critical.** There is no second Eureka node to sync with, so don't try to download a registry. Without this you get repeated `Cannot execute request on any known server` warnings in the console. |
| `eureka.client.service-url.defaultZone` | The registration URL. Client services in later phases will point at exactly this value: `http://localhost:8761/eureka/`. |
| `eureka.server.enable-self-preservation: false` | Development convenience — stopped services disappear from the dashboard promptly, which makes the demo behave predictably. |
| `eureka.server.eviction-interval-timer-in-ms: 15000` | Checks for dead instances every 15 seconds. |
| `management.endpoints...` | Exposes `/actuator/health` for verification. |

---

## 7. FILE: eureka-server/README.md

```markdown
# Eureka Server — PS004 Starlight Stays & Resorts

Service Discovery Server for the Multi-Property Hospitality Reservation & Management System.

- Course: 24SDCS03R — SOA Programming and Microservices
- Team: PS04-S06-02
- Phase: 2 of the project (Service Discovery)

## What this module does

This application is the service registry for the entire system. Every other
microservice built in later phases will register itself here on startup, and will
look up other services by logical name instead of by hardcoded IP/port.

It is a standalone Eureka Server: it does not register itself as a client and
does not fetch a registry from any peer server.

## Technology

| Component | Version |
|---|---|
| Java | 21 (17 also works) |
| Spring Boot | 3.5.15 |
| Spring Cloud | 2025.0.3 (Northfields) |
| Build tool | Maven |

## Configuration

| Property | Value |
|---|---|
| server.port | 8761 |
| spring.application.name | EUREKA-SERVER |
| eureka.client.register-with-eureka | false |
| eureka.client.fetch-registry | false |

## Run

    mvn clean install
    mvn spring-boot:run

Then open: http://localhost:8761

## Expected state in Phase 2

The dashboard will load and show no registered instances. This is correct —
no client services exist yet.

Services that will register in later phases:
- API-GATEWAY (Phase 3)
- USER-SERVICE (Phase 4)
- ROOM-SERVICE (Phase 5)
- BOOKING-SERVICE (Phase 6)

## Health check

    GET http://localhost:8761/actuator/health
    → {"status":"UP", ...}
```

### FILE: eureka-server/.gitignore

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

## 8. FILE: eureka-server/src/test/java/com/starlight/eureka/EurekaServerApplicationTests.java

```java
package com.starlight.eureka;

import com.netflix.eureka.EurekaServerContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that this application really starts as a Eureka Server and that the
 * standalone configuration required by the PS004 architecture is actually applied.
 *
 * A random port is used so the test never collides with a Eureka Server that the
 * student may already have running on 8761.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "eureka.instance.hostname=localhost"
)
class EurekaServerApplicationTests {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private Environment environment;

    @Autowired
    private EurekaClientConfigBean eurekaClientConfig;

    @Test
    @DisplayName("Spring application context loads")
    void contextLoads() {
        assertThat(context).isNotNull();
    }

    @Test
    @DisplayName("Application starts as a Eureka Server (registry context exists)")
    void eurekaServerContextIsCreated() {
        EurekaServerContext serverContext = context.getBean(EurekaServerContext.class);

        assertThat(serverContext).isNotNull();
        assertThat(serverContext.getRegistry()).isNotNull();
    }

    @Test
    @DisplayName("Server does not register itself as a Eureka client")
    void selfRegistrationIsDisabled() {
        assertThat(eurekaClientConfig.shouldRegisterWithEureka()).isFalse();
    }

    @Test
    @DisplayName("Server does not fetch a registry from any other Eureka Server")
    void registryFetchingIsDisabled() {
        assertThat(eurekaClientConfig.shouldFetchRegistry()).isFalse();
    }

    @Test
    @DisplayName("Application registers under the logical name EUREKA-SERVER")
    void applicationIdentityIsCorrect() {
        // Note: server.port is deliberately overridden to a random port during tests,
        // so it is not asserted here. The production value (8761) lives in application.yml.
        assertThat(environment.getProperty("spring.application.name")).isEqualTo("EUREKA-SERVER");
    }
}
```

### Why these tests are meaningful (not fake)

| Test | What would break it |
|---|---|
| `contextLoads` | Any bad dependency, bad YAML, or version mismatch. |
| `eurekaServerContextIsCreated` | Removing `@EnableEurekaServer` or using the wrong starter — `EurekaServerContext` only exists when the server auto-configuration actually activated. This is the strongest proof that the app is a **registry**, not just a web app. |
| `selfRegistrationIsDisabled` | Someone setting `register-with-eureka: true` or deleting the property. |
| `registryFetchingIsDisabled` | Someone setting `fetch-registry: true` or deleting the property. |
| `applicationIdentityIsCorrect` | A typo in `spring.application.name`, which would break naming conventions across phases. |

Run them with:

```bash
mvn test
```

---

## 9. VS CODE SETUP (Windows)

### Step 1 — Install / check Java

If you don't have a JDK, install **Eclipse Temurin JDK 21** (or JDK 17) from
`https://adoptium.net`. During install, tick **"Set JAVA_HOME variable"** and
**"Add to PATH"**.

### Step 2 — Check the Java version

Open a **new** Command Prompt or PowerShell (new, so it picks up the updated PATH):

```cmd
java -version
```

Expected output (something like):

```
openjdk version "21.0.x" ...
OpenJDK Runtime Environment Temurin-21.0.x ...
```

Also confirm the compiler and JAVA_HOME:

```cmd
javac -version
echo %JAVA_HOME%
```

If `JAVA_HOME` prints nothing, set it:
Windows Search → *"Edit the system environment variables"* → **Environment Variables** →
**New (System variables)** → Name `JAVA_HOME`, Value `C:\Program Files\Eclipse Adoptium\jdk-21...`

### Step 3 — Install VS Code extensions

In VS Code, open the Extensions panel (`Ctrl+Shift+X`) and install:

1. **Extension Pack for Java** (Microsoft) — compiler, debugger, IntelliSense
2. **Spring Boot Extension Pack** (VMware) — Spring-aware editing, Spring Boot Dashboard

### Step 4 — Open the project in VS Code

Important: open the **`eureka-server` folder itself**, not its parent.

```
File → Open Folder… → C:\...\PS004-Starlight-Stays\eureka-server
```

Wait for the status bar to finish *"Importing Maven project…"*. If it never triggers:
`Ctrl+Shift+P` → **Java: Clean Java Language Server Workspace** → Restart.

### Step 5 — Open the integrated terminal

```
Terminal → New Terminal   (or Ctrl + ` )
```

Confirm you are in the right folder — `dir` should list `pom.xml`.

### Step 6 — Build with Maven

```cmd
mvn clean install
```

Expected: `BUILD SUCCESS`. The first run downloads dependencies and can take 2–5 minutes.

### Step 7 — Start the Eureka Server

```cmd
mvn spring-boot:run
```

### Step 8 — Open the browser

```
http://localhost:8761
```

---

## 10. RUN COMMANDS — `mvn` vs the Maven Wrapper

**Which should you use?**

> **Use `mvn spring-boot:run`.** You already have Maven available through VS Code's Java
> extension pack, and this is the simplest, most reliable path for a local academic project.

### Option A — System Maven (recommended for you)

```cmd
mvn clean install
mvn spring-boot:run
```

If `mvn` is not recognised, either:
- install Maven from `https://maven.apache.org/download.cgi`, unzip to `C:\apache-maven-3.9.x`,
  and add `C:\apache-maven-3.9.x\bin` to your PATH, **or**
- use Option B below.

### Option B — Maven Wrapper

The Maven Wrapper (`mvnw`) is useful when you want the project to build with a pinned Maven
version on any machine, without Maven being installed. The wrapper files are **generated**, not
hand-written, so create them once from inside the `eureka-server` folder:

```cmd
mvn -N wrapper:wrapper -Dmaven=3.9.9
```

That produces `mvnw`, `mvnw.cmd` and `.mvn/wrapper/maven-wrapper.properties`. After that, the
**Windows command** is:

```cmd
mvnw.cmd clean install
mvnw.cmd spring-boot:run
```

(Note: on Windows use `mvnw.cmd`, not `./mvnw` — that's the Linux/macOS form.)

### Option C — VS Code UI

With the Spring Boot Extension Pack installed, open the **Spring Boot Dashboard** in the
sidebar, find `eureka-server`, and click ▶ **Run**. Equivalent to Option A, just with buttons.

### Option D — Run the built JAR

```cmd
mvn clean package
java -jar target\eureka-server-1.0.0.jar
```

### To stop the server

Press `Ctrl + C` in the terminal.

---

## 11. VERIFICATION

### Check 1 — Console output

Look for these lines (order approximate):

```
Started EurekaServerApplication in 6.4 seconds (process running for 7.1)
Tomcat started on port 8761 (http) with context path '/'
Setting the eureka configuration..
Initialized server context
Started Eureka Server
```

The two decisive lines are **`Tomcat started on port 8761`** and **`Started Eureka Server`**.

### Check 2 — Dashboard loads

Open `http://localhost:8761`. You should see the **"Spring Eureka"** page with:

- Header: *System Status* — showing Environment, Data center, Current time, Uptime
- Section: **Instances currently registered with Eureka**
- Section: *General Info* — total-avail-memory, num-of-cpus, current-memory-usage, etc.
- Section: *Instance Info* — `ipAddr`, `status: UP`

### Check 3 — Zero registered instances (expected in Phase 2)

Under **Instances currently registered with Eureka** you will see either an empty table or
the text **"No instances available"**.

**This is correct and expected.** The registry is a phone book; in Phase 2 the phone book
exists but nobody has listed a number yet. `API-GATEWAY`, `USER-SERVICE`, `ROOM-SERVICE` and
`BOOKING-SERVICE` do not exist yet and will appear in Phases 3–6.

### Check 4 — The server is NOT registering itself

This is the check evaluators like. Evidence:

1. The instances table is **empty** — if self-registration were on, you would see a row for
   `EUREKA-SERVER` listing itself.
2. The console contains **no** repeated warnings like
   `Cannot execute request on any known server` or `DiscoveryClient_EUREKA-SERVER ... registering service...`.
3. `application.yml` explicitly sets `register-with-eureka: false` and `fetch-registry: false`.

### Check 5 — Health endpoint

```
http://localhost:8761/actuator/health
```

Expected:

```json
{"status":"UP","components":{"diskSpace":{"status":"UP"},"ping":{"status":"UP"}}}
```

### Check 6 — Registry REST API is live

```
http://localhost:8761/eureka/apps
```

Expected: an XML document with an empty `<applications>` element (no `<application>` children
yet). This proves the registration endpoint that future services will POST to is already
working.

### Check 7 — Tests pass

```cmd
mvn test
```

Expected: `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0` → `BUILD SUCCESS`.

---

## 12. TROUBLESHOOTING (Windows + VS Code)

### A. Port 8761 already in use

**Symptom:** `Web server failed to start. Port 8761 was already in use.`

**Find and kill the process:**

```cmd
netstat -ano | findstr :8761
```

Note the PID in the last column, then:

```cmd
taskkill /PID <pid> /F
```

**Or** temporarily run on another port (do **not** commit this — Phase 1 fixes 8761):

```cmd
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8762
```

Most common cause: you already started the server in another terminal or via the Spring Boot
Dashboard and forgot to stop it.

---

### B. Java version problem

**Symptom:** `class file has wrong version 65.0, should be 61.0`, or
`invalid target release: 21`, or `Unsupported class file major version`.

**Fix:**

1. `java -version` and `echo %JAVA_HOME%` — both must point to JDK 17 or 21.
2. If you only have JDK 17, change `<java.version>21</java.version>` to `17` in `pom.xml`.
3. In VS Code: `Ctrl+Shift+P` → **Java: Configure Java Runtime** → set the project JDK.
4. Then `Ctrl+Shift+P` → **Java: Clean Java Language Server Workspace** → Restart.

A JRE is not enough — you need a full **JDK**.

---

### C. `mvn` is not recognized

**Symptom:** `'mvn' is not recognized as an internal or external command`

**Fix (pick one):**

1. Install Maven, add `C:\apache-maven-3.9.x\bin` to PATH, then **open a new terminal**
   (existing terminals keep the old PATH).
2. Generate and use the wrapper: `mvnw.cmd spring-boot:run` (see Section 10, Option B).
3. Skip the CLI entirely — use the **Spring Boot Dashboard** ▶ Run button in VS Code.

---

### D. Dependency resolution error

**Symptom:** `Could not resolve dependencies`, `Failed to read artifact descriptor`,
`Non-resolvable import POM`.

**Fixes in order:**

1. Check internet access; corporate/college networks often block Maven Central.
2. Force a re-download:
   ```cmd
   mvn clean install -U
   ```
3. Clear a corrupted local cache (safe — it re-downloads):
   ```cmd
   rmdir /s /q "%USERPROFILE%\.m2\repository\org\springframework\cloud"
   mvn clean install
   ```
4. If you are behind a proxy, configure it in `%USERPROFILE%\.m2\settings.xml`.
5. Verify you typed the artifact id correctly:
   `spring-cloud-starter-netflix-eureka-**server**` (not `-client`).

---

### E. Spring Boot / Spring Cloud compatibility error

**Symptom:**
```
Spring Cloud Version 2024.0.x is not compatible with Spring Boot Version 3.5.15
```

**Fix:** Do **not** disable the verifier. Fix the versions instead — in `pom.xml` confirm:

```xml
<parent> ... <version>3.5.15</version> </parent>
<spring-cloud.version>2025.0.3</spring-cloud.version>
```

Also make sure no `spring-cloud-*` dependency has its own hardcoded `<version>` tag — the BOM
must be the only thing deciding those versions.

---

### F. Application fails to start

**Symptom:** stack trace immediately after `Starting EurekaServerApplication`.

**Checklist:**

| Cause | Fix |
|---|---|
| YAML indentation error (`ScannerException` / `mapping values are not allowed here`) | YAML uses **spaces, never tabs**. Re-check `application.yml` indentation. |
| `@EnableEurekaServer` cannot be resolved | Wrong starter (`-client` instead of `-server`), or Maven not re-imported. Run `mvn clean install`, then reload VS Code. |
| Class not found / no main manifest | Ensure `EurekaServerApplication.java` is under `src/main/java/com/starlight/eureka/` and its `package` line matches exactly. |
| Stale build | `mvn clean` then rebuild. |

---

### G. Eureka dashboard not opening

**Symptom:** browser shows *"This site can't be reached"* or a blank page.

**Checklist:**

1. Is the app still running? The terminal must **not** have returned to the prompt.
2. Did the console print `Tomcat started on port 8761`? If it printed a different port, use that.
3. Use `http://localhost:8761` — **not** `https://`. There is no TLS configured.
4. Try `http://127.0.0.1:8761` in case of a hosts-file/IPv6 quirk.
5. Hard-refresh with `Ctrl + F5` to bypass a cached error page.
6. Temporarily allow Java through Windows Defender Firewall if prompted on first run.

---

### H. Dashboard loads but shows nothing under "Instances"

**This is not an error in Phase 2.** See Verification Check 3. No client services have been
built yet.

---

## 13. SCREENSHOT EVIDENCE

Capture these and save them to `docs/screenshots/`.

### Screenshot 1 — `phase2_eureka_dashboard.png`

**What to capture:** Full browser window at `http://localhost:8761`, with the URL bar visible,
showing the Spring Eureka page including the *System Status* block and the
*Instances currently registered with Eureka* section.

**Evidence it provides:** The Eureka Server is running on the architecturally-specified port
8761 and its dashboard is live. This is the primary artefact for the
*"Microservice Identification and Service Discovery"* rubric.

**What to say:** *"This is our service registry running on port 8761. In Phase 2 we have only
built the registry itself, so no instances are registered yet — the client services register in
later phases."*

---

### Screenshot 2 — `phase2_console_startup.png`

**What to capture:** The VS Code integrated terminal showing the startup log, with
`Tomcat started on port 8761`, `Started Eureka Server`, and
`Started EurekaServerApplication in X seconds` all visible.

**Evidence it provides:** The application genuinely boots as a Eureka Server (not just any web
app), on the correct port, with no startup errors.

---

### Screenshot 3 — `phase2_no_self_registration.png`

**What to capture:** A split or side-by-side of `application.yml` (showing
`register-with-eureka: false` and `fetch-registry: false`) next to the dashboard's empty
instances table.

**Evidence it provides:** The standalone configuration is intentional and correctly applied —
the server does not register with or fetch from itself. Evaluators frequently ask about this.

---

### Screenshot 4 — `phase2_tests_passing.png`

**What to capture:** Terminal output of `mvn test` showing
`Tests run: 5, Failures: 0, Errors: 0` and `BUILD SUCCESS`.

**Evidence it provides:** The configuration is verified automatically, not just visually.

---

### Screenshot 5 (optional) — `phase2_eureka_apps_empty.png`

**What to capture:** Browser at `http://localhost:8761/eureka/apps` showing the empty
`<applications>` XML.

**Evidence it provides:** The registration REST API that future services will call is already
operational.

> **Do not claim** in your report that `USER-SERVICE`, `ROOM-SERVICE`, `BOOKING-SERVICE` or
> `API-GATEWAY` are registered. They do not exist yet. Screenshots showing them registered
> belong to Phases 3–6.

---

## 14. RUBRIC MAPPING — "Microservice Identification and Service Discovery"

### What Eureka is

Eureka is a **service registry** — a dedicated server where microservice instances announce
themselves ("I am `ROOM-SERVICE`, reachable at 192.168.1.7:8082") and where other components
can ask "where is `ROOM-SERVICE` right now?". It has two halves:

- **Eureka Server** — holds the registry (this phase).
- **Eureka Client** — a library embedded in each microservice that registers on startup, sends
  a heartbeat every 30 seconds, and caches a copy of the registry locally.

### Why we need it

Phase 1 identified five components with independent lifecycles. In any real deployment their
addresses are not stable:

- A service restarts and Spring picks a different ephemeral port.
- A service is redeployed to a different machine or container and gets a new IP.
- A service is **scaled** — now two or three instances exist under one name.

Hardcoding `http://localhost:8082` in the Gateway and in Booking Service would mean editing and
redeploying code every time any of those happens. Eureka removes that coupling entirely: code
refers to a **name**, and the name is resolved at request time.

### How it supports our Phase 1 architecture

| Phase 1 design element | Depends on Eureka how |
|---|---|
| Gateway route `lb://ROOM-SERVICE` | The `lb://` load-balanced scheme asks the discovery client to resolve `ROOM-SERVICE` to a live instance before forwarding. |
| Booking Service → Room Service via OpenFeign | The Feign client is declared against the name `ROOM-SERVICE`; discovery supplies the actual address. |
| NFR: Scalability | Multiple instances registering under the same name gives client-side load balancing for free. |
| NFR: Availability | If one instance of a service dies, Eureka evicts it and traffic goes to the surviving instances. |
| NFR: Maintainability | Adding a new microservice later requires no change to existing services' configuration. |

### What will register later

| Service | Eureka name | Port | Phase |
|---|---|---|---|
| API Gateway | `API-GATEWAY` | 8080 | 3 |
| User Service | `USER-SERVICE` | 8081 | 4 |
| Room Service | `ROOM-SERVICE` | 8082 | 5 |
| Booking Service | `BOOKING-SERVICE` | 8083 | 6 |

Each will add `spring-cloud-starter-netflix-eureka-client` and point
`eureka.client.service-url.defaultZone` at `http://localhost:8761/eureka/` — exactly the URL
this server exposes.

### Why this beats hardcoding IP addresses in *this* project

1. **Peak-season scaling.** Phase 1's NFRs call for Room Service (read-heavy search) and
   Booking Service (write-heavy) to scale independently. With hardcoded URLs, adding a second
   Room Service instance means changing the Gateway config and restarting it. With Eureka, the
   new instance simply registers and starts receiving traffic.
2. **Failure handling.** A crashed instance stops sending heartbeats and is evicted, so callers
   stop being routed to a dead address. A hardcoded URL keeps pointing at the corpse.
3. **Single source of truth.** Service locations live in one registry, not scattered across
   four `application.yml` files.
4. **Environment portability.** The same build runs on a laptop, a lab machine, or a container
   host with no URL edits — only the `defaultZone` changes.
5. **Demonstrable evidence.** The dashboard gives the evaluator a live, visual proof of the
   discovery mechanism, which a hardcoded-URL design simply cannot show.

### Marks-level demonstration

Show the dashboard running now (Phase 2), then in the final demo show all four clients
registered simultaneously, stop one service, refresh, and show it disappear from the registry.
That live before/after is the strongest possible evidence for this rubric.

---

## 15. VIVA QUESTIONS — EUREKA

**1. What is Eureka?**
Eureka is a service registry from Netflix, provided in Spring Cloud. It's a server where
microservices register themselves, so other services can find them by name instead of by IP
address and port.

**2. Why do we need service discovery in this project?**
Because our Gateway and Booking Service need to call other services, and those services' IPs and
ports change when they restart, redeploy, or scale. Service discovery lets us refer to them by a
fixed logical name like `ROOM-SERVICE` instead of an address that keeps changing.

**3. What is service registration?**
When a microservice starts, its Eureka client sends its name, host, port and status to the
Eureka Server, which stores it in the registry. The service then sends a heartbeat every 30
seconds to prove it's still alive.

**4. What is service discovery?**
It's the lookup side: a client asks Eureka for a service name, and Eureka returns the current
list of live instances. Spring then picks one — that's how `lb://ROOM-SERVICE` in our Gateway
resolves to a real address.

**5. What happens when a service's port or IP changes?**
Nothing breaks. When the service restarts on the new address, it re-registers with Eureka using
the new host and port, and the old dead entry is evicted after heartbeats stop. Callers still
use the same service name, so no code or config anywhere else has to change.

**Bonus — Why does the Eureka Server itself have `register-with-eureka: false`?**
Because this application *is* the registry. Registering with itself would add a meaningless
entry to the dashboard and cause unnecessary self-connection attempts. `fetch-registry: false`
is set for the same reason — there's no peer server to download a registry from.

---

## 16. FINAL CONSISTENCY CHECK

| Check | Status |
|---|---|
| Java version is compatible | ✅ Java 21 (17 supported via one property change); Spring Boot 3.5.x requires 17+ |
| Spring Boot version is compatible | ✅ 3.5.15 |
| Spring Cloud version is compatible | ✅ 2025.0.3 (Northfields), officially based on Spring Boot 3.5.15 |
| Maven dependencies are correct | ✅ Eureka server starter + actuator + test only; BOM-managed versions, no hardcoded cloud versions |
| Application class is correct | ✅ `com.starlight.eureka.EurekaServerApplication` with correct package and imports |
| Eureka annotation is correct | ✅ `@EnableEurekaServer` (server, not client) |
| Port is 8761 | ✅ `server.port: 8761` |
| Application name is EUREKA-SERVER | ✅ `spring.application.name: EUREKA-SERVER` |
| Self-registration is disabled | ✅ `eureka.client.register-with-eureka: false` |
| Registry fetching is disabled | ✅ `eureka.client.fetch-registry: false` |
| No unnecessary services generated | ✅ Only `eureka-server` exists |
| No JWT generated | ✅ No security/JWT dependency or code |
| No database generated | ✅ No JPA, no MySQL driver, no entities |
| No OpenFeign / Gateway generated | ✅ Deferred to Phases 3 and 6 |
| Project can run independently | ✅ Standalone; depends on nothing else |
| Dashboard opens at localhost:8761 | ✅ Provided by the Eureka server starter |
| Folder structure is complete | ✅ pom.xml, main class, application.yml, test, README, .gitignore |
| No missing imports | ✅ All imports present in both Java files |
| No placeholder code | ✅ Every file is complete and compilable |

### One honest note on the build environment

These files were authored and structured here, but **the Maven build could not be executed in
this environment** (no Maven binary and no access to Maven Central from the sandbox). The
version pairing was verified against Spring's own release announcements. You should run
`mvn clean install` on your Windows machine as the real compile check — Section 12 covers every
error you're likely to hit.

---

**PHASE 2 COMPLETE — READY FOR PHASE 3: API GATEWAY**
