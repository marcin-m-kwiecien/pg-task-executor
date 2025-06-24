package xyz.kwiecien.experiments.pgworkerpool.jobs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import xyz.kwiecien.experiments.pgworkerpool.api.JobSubmitRequest;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static xyz.kwiecien.experiments.pgworkerpool.ExceptionUtil.stackTraceToString;


@Repository
public class PGJobsRepository implements JobsRepository {
    private static final String SUBMIT_JOB_QUERY = """
            INSERT INTO jobs (job_type, payload, status, start_after)
            VALUES (:jobType, CAST(:payload AS JSONB), :status, :startAfter)""";

    private static final String GET_JOB_TO_RUN_QUERY = """
            WITH to_acquire AS (
                SELECT id FROM jobs WHERE (
                    (status = 'Pending') 
                        OR (status = 'InProgress' AND expires_at < NOW())
                        OR (status = 'Failed' AND expires_at < NOW() AND attempt < :maxAttempts))
                AND (start_after IS NULL OR start_after <= NOW())
                AND job_type = :jobType
                ORDER BY created_at ASC
                LIMIT 1
                FOR UPDATE SKIP LOCKED
            )
            UPDATE jobs SET
                status = 'InProgress',
                started_at = NOW(),
                expires_at = :expiresAt,
                worker_id = :workerId
            FROM to_acquire
            WHERE jobs.id = to_acquire.id
            RETURNING jobs.id, jobs.job_type, jobs.payload, jobs.created_at, jobs.expires_at, jobs.attempt, jobs.last_error""";
    private static final String UPDATE_SUCCESSFUL_JOB = """
            UPDATE jobs SET
              status = 'Completed',
              completed_at = NOW()
            WHERE id = :jobId
            """;
    private static final String UPDATE_FAILED_JOB = """
                    UPDATE jobs SET
                        attempt = attempt + 1,
                        status = 'Failed',
                        last_error = :error
                    WHERE id = :jobId
            """;
    private static final TypeReference<Map<String, Object>> MAP_TYPE_REF = new TypeReference<>() {};
    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public PGJobsRepository(JdbcClient jdbcClient,
                            ObjectMapper objectMapper,
                            PlatformTransactionManager txManager) {
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(txManager);
    }

    @Override
    public void submitJob(JobSubmitRequest request) {
        try {
            jdbcClient.sql(SUBMIT_JOB_QUERY)
                    .param("jobType", request.jobType())
                    .param("payload", objectMapper.writeValueAsString(request.jobParameters()))
                    .param("status", "Pending")
                    .param("startAfter", Timestamp.from(request.startAfter()))
                    .update();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void executeForNextJob(String jobType, String workerId, Duration maxDuration, JobRunner runner) {
        var expiresAt = Instant.now().plus(maxDuration);
        transactionTemplate.executeWithoutResult(_ -> {
            var nextJob = jdbcClient.sql(GET_JOB_TO_RUN_QUERY)
                    .param("jobType", jobType)
                    .param("expiresAt", Timestamp.from(expiresAt))
                    .param("maxAttempts", 5)
                    .param("workerId", workerId)
                    .query(this::mapJobInfo)
                    .optional();
            if (nextJob.isEmpty()) {
                return;
            }
            try {
                runner.run(nextJob.get());
                jdbcClient.sql(UPDATE_SUCCESSFUL_JOB)
                        .param("jobId", nextJob.get().id())
                        .update();
            } catch (Exception e) {
                var stackTrace = stackTraceToString(e);
                jdbcClient.sql(UPDATE_FAILED_JOB)
                        .param("jobId", nextJob.get().id())
                        .param("error", e.getMessage() + "\n" + stackTrace)
                        .update();
            }
        });
    }

    private JobInfo mapJobInfo(ResultSet rs, int rowNum) throws SQLException {
        var payload = rs.getString(3);
        try {
            var payloadMap = objectMapper.readValue(payload, MAP_TYPE_REF);
            return new JobInfo(
                    rs.getInt(1),
                    rs.getString(2),
                    payloadMap,
                    rs.getTimestamp(4).toInstant(),
                    rs.getTimestamp(5).toInstant(),
                    rs.getInt(6),
                    rs.getString(7));
        } catch (JsonProcessingException e) {
            throw new SQLException(e);
        }
    }
}
