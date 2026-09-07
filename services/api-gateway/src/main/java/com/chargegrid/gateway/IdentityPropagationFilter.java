package com.chargegrid.gateway;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Establishes the trust boundary for caller identity.
 *
 * <p>Downstream services identify the driver from {@code X-User-Id}. They are ClusterIP-only, so
 * the gateway is the sole path in, and this filter is the only thing allowed to set that header:
 * any value a client sends is dropped first, then the subject of the validated access token is
 * written in its place. Without the strip, a driver holding a perfectly valid token for their own
 * account could read someone else's reservations just by sending another id.
 */
@Component
public class IdentityPropagationFilter implements GlobalFilter, Ordered {

    static final String USER_ID_HEADER = "X-User-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                .map(token -> token.getToken().getSubject())
                .defaultIfEmpty("")
                .flatMap(subject -> chain.filter(withCallerIdentity(exchange, subject)));
    }

    private ServerWebExchange withCallerIdentity(ServerWebExchange exchange, String subject) {
        ServerHttpRequest.Builder request =
                exchange.getRequest().mutate().headers(headers -> headers.remove(USER_ID_HEADER));
        if (!subject.isEmpty()) {
            request.header(USER_ID_HEADER, subject);
        }
        return exchange.mutate().request(request.build()).build();
    }

    /**
     * Runs just before the routing filter that actually forwards the request, so the headers it
     * rewrites are the ones the downstream service receives.
     */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 1;
    }
}
