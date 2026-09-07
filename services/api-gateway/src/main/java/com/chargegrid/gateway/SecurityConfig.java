package com.chargegrid.gateway;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    @Profile("!test")
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(Customizer.withDefaults())
                .authorizeExchange(
                        exchanges ->
                                exchanges
                                        // A CORS preflight carries no Authorization header, so it
                                        // has to pass before authentication or every cross-origin
                                        // call from the SPA fails.
                                        .pathMatchers(HttpMethod.OPTIONS)
                                        .permitAll()
                                        .pathMatchers("/actuator/health", "/actuator/info")
                                        .permitAll()
                                        .anyExchange()
                                        .authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
                .build();
    }

    @Bean
    @Profile("test")
    SecurityWebFilterChain testSecurityWebFilterChain(ServerHttpSecurity http) {
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(Customizer.withDefaults())
                .authorizeExchange(
                        exchanges ->
                                exchanges
                                        .pathMatchers("/actuator/health", "/actuator/info")
                                        .permitAll()
                                        .anyExchange()
                                        .permitAll())
                .build();
    }

    /**
     * Single source of CORS configuration. The gateway's own {@code globalcors} block is
     * deliberately not used as well: two layers each adding {@code Access-Control-Allow-Origin}
     * produces duplicate headers, which browsers reject.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${chargegrid.cors.allowed-origins}") String allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(
                Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
