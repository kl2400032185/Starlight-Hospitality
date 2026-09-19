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
                .extracting(route -> route.getId())
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
