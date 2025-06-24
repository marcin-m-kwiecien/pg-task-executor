package xyz.kwiecien.experiments.pgworkerpool.jobs;

import xyz.kwiecien.experiments.pgworkerpool.api.JobSubmitRequest;

import java.time.Duration;

public interface JobsRepository {
    void submitJob(JobSubmitRequest request);

    void executeForNextJob(String jobType, String workerId, Duration maxDuration, JobRunner runner);

    interface JobRunner {
        void run(JobInfo job) throws Exception;
    }
}
