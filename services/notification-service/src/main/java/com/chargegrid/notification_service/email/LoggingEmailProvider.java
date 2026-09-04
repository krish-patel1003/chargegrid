package com.chargegrid.notification_service.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(EmailProvider.class)
public class LoggingEmailProvider implements EmailProvider {
    private static final Logger log = LoggerFactory.getLogger(LoggingEmailProvider.class);

    @Override
    public String send(EmailMessage message) {
        log.info("Email delivery (no-op provider): to={}, subject={}", message.to(), message.subject());
        return "log-provider";
    }
}
