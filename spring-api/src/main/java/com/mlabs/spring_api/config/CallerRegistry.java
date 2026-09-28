package com.mlabs.spring_api.config;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Optional;

@Component
public class CallerRegistry {
    public record Caller(String tenant, String role) {}
    private final Map<String, Caller> callers = Map.of(
        "atlas-employee-01", new Caller("Atlas", "employee"),
        "atlas-contractor-01", new Caller("Atlas", "contractor"),
        "boreal-employee-01", new Caller("Boreal", "employee")
    );
    public Optional<Caller> find(String callerId) { return Optional.ofNullable(callers.get(callerId)); }
}
