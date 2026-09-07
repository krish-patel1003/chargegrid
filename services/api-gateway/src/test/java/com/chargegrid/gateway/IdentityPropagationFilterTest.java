package com.chargegrid.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

class IdentityPropagationFilterTest {

    private static final String SUBJECT = "4f1c9a6e-0000-4000-8000-000000000001";

    private final IdentityPropagationFilter filter = new IdentityPropagationFilter();

    @Test
    void writesTheTokenSubjectAsTheCallerIdentity() {
        ServerWebExchange forwarded =
                forward(MockServerHttpRequest.get("/api/reservations"), SUBJECT);

        assertThat(forwarded.getRequest().getHeaders().get("X-User-Id")).containsExactly(SUBJECT);
    }

    @Test
    void discardsACallerSuppliedIdentityInFavourOfTheToken() {
        ServerWebExchange forwarded =
                forward(
                        MockServerHttpRequest.get("/api/reservations")
                                .header("X-User-Id", "someone-elses-account"),
                        SUBJECT);

        assertThat(forwarded.getRequest().getHeaders().get("X-User-Id")).containsExactly(SUBJECT);
    }

    @Test
    void stripsACallerSuppliedIdentityWhenThereIsNoToken() {
        ServerWebExchange forwarded =
                forward(
                        MockServerHttpRequest.get("/api/reservations")
                                .header("X-User-Id", "someone-elses-account"),
                        null);

        assertThat(forwarded.getRequest().getHeaders().headerNames()).doesNotContain("X-User-Id");
    }

    @Test
    void isNotConfusedByHeaderCasing() {
        ServerWebExchange forwarded =
                forward(
                        MockServerHttpRequest.get("/api/reservations")
                                .header("x-user-id", "someone-elses-account"),
                        SUBJECT);

        assertThat(forwarded.getRequest().getHeaders().get("X-User-Id")).containsExactly(SUBJECT);
    }

    /** Runs the filter and returns the exchange it handed to the rest of the chain. */
    private ServerWebExchange forward(
            MockServerHttpRequest.BaseBuilder<?> request, String subject) {
        MockServerWebExchange exchange = MockServerWebExchange.from(request.build());
        var captured = new ServerWebExchange[1];
        Mono<Void> result =
                filter.filter(
                        exchange,
                        forwardedExchange -> {
                            captured[0] = forwardedExchange;
                            return Mono.empty();
                        });
        if (subject != null) {
            result = result.contextWrite(authenticatedAs(subject));
        }
        result.block();
        return captured[0];
    }

    private Context authenticatedAs(String subject) {
        Jwt jwt =
                Jwt.withTokenValue("token")
                        .header("alg", "none")
                        .subject(subject)
                        .claim("scope", "openid")
                        .build();
        var authentication = new JwtAuthenticationToken(jwt, List.of());
        return org.springframework.security.core.context.ReactiveSecurityContextHolder
                .withSecurityContext(Mono.just(new SecurityContextImpl(authentication)));
    }
}
