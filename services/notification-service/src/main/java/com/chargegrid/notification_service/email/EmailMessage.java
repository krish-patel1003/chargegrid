package com.chargegrid.notification_service.email;

public record EmailMessage(String from, String to, String subject, String text) {}
