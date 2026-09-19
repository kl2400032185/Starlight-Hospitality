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
