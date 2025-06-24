package xyz.kwiecien.experiments.pgworkerpool.jobs;

import java.time.Instant;
import java.util.Map;

public record JobInfo(
        int id,
        String jobType,
        Map<String, Object> payload,
        Instant createdAt,
        Instant expiresAt,
        int attempt,
        String lastError) {
}
