package com.cfgtest.services.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Duration;
import java.util.HashSet;

@Profile("EnableLoadBalancedConfig")
@Configuration
// Registers application with Eureka server.
@EnableEurekaClient
@Slf4j
public class LoadBalancedGatewayConfig {

    // Default circuit breaker configuration using Resilience4J. ( alt to hystrix as its in
    // maintenance mode )
    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer()
    {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
                .timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(30)).build()).build());
    }

    // Functional endpoints.. Router Function and handler function.
    // Spring cloud gateway provides RouteLocator to define routes and global filters
    @Bean
    public RouteLocator customerServiceRouteConfig(RouteLocatorBuilder builder) {
        final HashSet<String> cbCodes = new HashSet<>();
        cbCodes.add("500");
        return builder
                .routes()
                .route("GetCustomerById",
                        route -> route
//                                    AntPathMatcher pattern
                                .path("/customerDetails/**","/customerDetails")
//                                    circuit breaker configuration to fail if /customerList or
//                                    the proxy service returns a failure.
                                .filters(f-> {
                                            log.info("Within filters..");
                                            return f
                                                    .circuitBreaker(config -> {
                                                        log.info("GetCustomerByIdCircuitBreaker " +
                                                                "filter");
                                                        config.setName("GetCustomerByIdCircuitBreaker")
                                                                .setFallbackUri("forward:/fallback")
                                                                .setStatusCodes(cbCodes)
                                                                .setRouteId("GetCustomerById" +
                                                                        "-Fallback");
                                                    })
                                                    .rewritePath("/customerDetails(?<segment>.*)",
                                                            "/customer${segment}");
//                                              override path from customerList to /customers
//                                                .setPath("/customers")

                                        }
                                )
                                // uses spring cloud LoadBalancerClient to resolve the name to an
                                // actual host and port
                                .uri("lb://customer-service")
                )
                .route("GetCustomerList",
                        route -> route
                                .path("/customerList")
                                //                                    circuit breaker configuration to fail if /customerList or
                                //                                    the proxy service returns a failure.
                                .filters(f-> f
                                        .circuitBreaker(config -> {
                                            log.info("CustomerListCircuitBreaker " +
                                                    "filter");
                                            config.setName("CustomerListCircuitBreaker")
                                                    .setFallbackUri("forward:/fallback")
                                                    .setStatusCodes(cbCodes)
                                                    .setRouteId("CustomerListCircuitBreaker" +
                                                            "-Fallback");
                                        })
                                        //                                              override path from customerList to /customers
                                        .setPath("/customers")
                                )
                                // uses spring cloud LoadBalancerClient to resolve the name to an
                                // actual host and port
                                .uri("lb://customer-service")
                )
                .route("Risk Check",
                        route -> route
                                .path("/riskCheck/**","/riskCheck")
                                // uses spring cloud LoadBalancerClient to resolve the name to an
                                // actual host and port
                                .filters(f-> f.circuitBreaker(config -> config
                                        .setName("RiskCheckCircuitBreaker")
                                        // Failover defined within Risk check service.
                                        .setFallbackUri("forward:/riskcheck-failover")
                                                .setStatusCodes(cbCodes)
                                                .setRouteId("RiskCheckCircuitBreaker" +
                                                        "-Fallback")
                                        )
                                )
                                .uri("lb://risk-evaluation-service")
                )
                .route("Risk Check Failover",
                        route -> route
                                .path("/riskcheck-failover")
                                .uri("lb://risk-evaluation-service")
                )
                .build();
    }
}
