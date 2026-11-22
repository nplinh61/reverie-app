package com.reverie.web.auth;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves the client IP used as a rate-limit key.
 *
 * <p>It uses only {@link HttpServletRequest#getRemoteAddr()} and deliberately
 * ignores {@code X-Forwarded-For}, which a client can spoof. Honoring forwarded
 * headers safely requires trusted-proxy configuration, which arrives with the
 * login work in a later phase (SECURITY.md section 7). Until then, trusting the
 * header would let an attacker bypass the limit by forging it.
 */
final class ClientIp {

    private ClientIp() {
    }

    static String from(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr != null && !remoteAddr.isBlank()) ? remoteAddr : "unknown";
    }
}
