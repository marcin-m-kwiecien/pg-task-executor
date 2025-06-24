package xyz.kwiecien.experiments.pgworkerpool.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.kwiecien.experiments.pgworkerpool.jobs.JobsRepository;

@RestController
@RequestMapping("/jobs")
public class JobSubmitController {
    private final JobsRepository jobsRepository;

    public JobSubmitController(JobsRepository jobsRepository) {
        this.jobsRepository = jobsRepository;
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submit(@RequestBody JobSubmitRequest request) {
        jobsRepository.submitJob(request);
        return ResponseEntity.noContent().build();
    }
}
