package com.chargegrid.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

/**
 * Fetches signing keys over the internal network while still validating that tokens were issued by
 * the public Keycloak URL the browser signed in against.
 *
 * <p>Letting {@code issuer-uri} drive discovery would tie both to one hostname, and in Docker they
 * are never the same: the browser reaches Keycloak at localhost, this service at {@code keycloak}.
 * Tokens would then be rejected for a mismatched {@code iss}.
 */
@Configuration
@Profile("!test")
public class JwtDecoderConfig {

    @Bean
    ReactiveJwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri) {
        NimbusReactiveJwtDecoder decoder =
                NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));
        return decoder;
    }
}
