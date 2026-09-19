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

import java.util.Objects;

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

            String routeAttribute = Objects.requireNonNull(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            Route route = exchange.getAttribute(routeAttribute);
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
