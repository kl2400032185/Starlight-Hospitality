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
