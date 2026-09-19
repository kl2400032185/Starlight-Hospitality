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
