package com.chargegrid.session_service.web;

import org.springframework.http.HttpStatus;

/** Domain failure carrying the HTTP status the caller should see. */
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
