package com.chargegrid.notification_service.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Stand-in provider that logs instead of sending, so the service runs without a Resend key.
 *
 * <p>The condition is the exact inverse of {@link ResendEmailProvider}'s rather than
 * {@code @ConditionalOnMissingBean}: that annotation is only evaluated for {@code @Bean} methods in
 * auto-configuration, so on a {@code @Component} it silently does nothing and the application
 * starts with no EmailProvider at all.
 */
@Component
@ConditionalOnExpression("'${notification.email.resend.api-key:}' == ''")
public class LoggingEmailProvider implements EmailProvider {
    private static final Logger log = LoggerFactory.getLogger(LoggingEmailProvider.class);

    @Override
    public String send(EmailMessage message) {
        log.info(
                "Email delivery (no-op provider): to={}, subject={}",
                message.to(),
                message.subject());
        return "log-provider";
    }
}
