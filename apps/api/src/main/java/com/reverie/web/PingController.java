package com.reverie.web;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lightweight liveness endpoint. Independent of the database and any domain
 * logic, so it answers even when other parts of the system are not ready.
 */
@RestController
public class PingController {

    @GetMapping("/api/v1/ping")
    public Map<String, String> ping() {
        return Map.of("status", "ok");
    }
}
