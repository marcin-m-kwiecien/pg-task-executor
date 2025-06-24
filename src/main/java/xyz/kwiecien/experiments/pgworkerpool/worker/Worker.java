package xyz.kwiecien.experiments.pgworkerpool.worker;

import org.slf4j.Logger;
import xyz.kwiecien.experiments.pgworkerpool.jobs.JobInfo;
import xyz.kwiecien.experiments.pgworkerpool.jobs.JobsRepository;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import static org.slf4j.LoggerFactory.getLogger;

public class Worker {
    private static final Logger log = getLogger(Worker.class);
    private final String id;
    private final JobsRepository jobsRepository;
    private volatile boolean running = false;


    public Worker(String id, JobsRepository jobsRepository) {
        this.id = id;
        this.jobsRepository = jobsRepository;
    }

    public void start() {
        this.running = true;
        Thread.ofVirtual().name("Worker-" + id).start(this::run);
    }

    public void stop() {
        this.running = false;
    }

    private void run() {
        while (running) {
            try {
                Thread.sleep(10000);
                log.info("[W:{}] Querying for the next job.", id);
                jobsRepository.executeForNextJob("SomeJob", id, Duration.ofSeconds(60), this::runJob);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void runJob(JobInfo jobInfo) {
        if (jobInfo.lastError() != null) {
            log.info("[W:{}] Previously failed {} times job {} with parameters {} started by worker. Will run for at least 5 seconds",
                    id,
                    jobInfo.attempt(),
                    jobInfo.id(),
                    jobInfo.payload());
        } else {
            log.info("[W:{}] Job {} with parameters {} started. Will run for at least 5 seconds",
                    id,
                    jobInfo.id(),
                    jobInfo.payload());
        }
        try {
            while (true) {
                Thread.sleep(5000);
                double rand = ThreadLocalRandom.current().nextDouble();
                if (rand > 0.9) {
                    log.error("[W:{}] Job {} failed.", id, jobInfo.id());
                    throw new RuntimeException("Job failed");
                } else if (rand < 0.5) {
                    log.info("[W:{}] Job {} finished.", id, jobInfo.id());
                    break;
                } else {
                    log.info("[W:{}] Job {} needs a bit more time...", id, jobInfo.id());
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
