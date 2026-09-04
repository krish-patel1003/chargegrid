package com.chargegrid.notification_service.email;

public interface EmailProvider {
    String send(EmailMessage message);
}
