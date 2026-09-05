package com.chargegrid.session_service.service;

import java.security.SecureRandom;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Issues and verifies the six-digit codes a charger displays. Codes are short enough to read off a
 * screen, so the guess budget on a reservation ({@code Reservation.MAX_FAILED_ATTEMPTS}) is what
 * actually bounds an attacker; hashing keeps a database dump from handing over live codes.
 */
@Component
public class AccessCodes {

    private static final int CODE_BOUND = 1_000_000;

    private final SecureRandom random = new SecureRandom();
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String generate() {
        return String.format("%06d", random.nextInt(CODE_BOUND));
    }

    public String hash(String code) {
        return encoder.encode(code);
    }

    public boolean matches(String code, String hash) {
        return encoder.matches(code, hash);
    }
}
