package com.cfgtest.services.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Duration;

@Profile("!EnableLoadBalancedConfig")
@Configuration
public class CfgServiceGatewayConfig {


    // Default circuit breaker configuration using Resilience4J. ( alt to hystrix as its in
    // maintainence mode )
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
        return builder
                    .routes()
                    .route("GetCustomerById",
                            route -> route
//                                    AntPathMatcher pattern
                                    .path("/customerDetails/**","/customerDetails")
//                                    circuit breaker configuration to fail if /customerList or
//                                    the proxy service returns a failure.
                                    .filters(f-> f
                                                .circuitBreaker(config -> {
                                                                    config.setName("circuitBreaker")
                                                                            .setFallbackUri("forward:/fallback");
                                                                })
                                                .rewritePath("/customerDetails(?<segment>.*)",
                                                        "/customer${segment}")
//                                              override path from customerList to /customers
//                                                .setPath("/customers")
                                                )

                                    .uri("http://localhost:8080")
                    )
                    .route("GetCustomerList",
                            route -> route
                                    .path("/customerList")
    //                                    circuit breaker configuration to fail if /customerList or
    //                                    the proxy service returns a failure.
                                    .filters(f-> f
                                                    .circuitBreaker(config -> {
                                                        config.setName("circuitBreaker")
                                                                .setFallbackUri("forward:/fallback");
                                                    })
    //                                              override path from customerList to /customers
                                                    .setPath("/customers")
                                    )
                                    .uri("http://localhost:8080")
                    )
                    .build();
    }

}
