package xyz.kwiecien.experiments.pgworkerpool.api;

import java.time.Instant;
import java.util.Map;

public record JobSubmitRequest(
        String jobType,
        Map<String, Object> jobParameters,
        Instant startAfter) {
}
