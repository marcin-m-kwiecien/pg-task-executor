package xyz.kwiecien.experiments.pgworkerpool.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import xyz.kwiecien.experiments.pgworkerpool.jobs.JobsRepository;
import xyz.kwiecien.experiments.pgworkerpool.worker.Worker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/workers")
public class WorkersController {
    private final JobsRepository jobsRepository;
    private final Map<String, Worker> workers = new ConcurrentHashMap<>();

    public WorkersController(JobsRepository jobsRepository) {
        this.jobsRepository = jobsRepository;
    }

    @PostMapping("/start")
    public ResponseEntity<?> startWorker(@RequestBody WorkerRequest request) {
        if (workers.containsKey(request.id())) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "worker with this id already exists")
            );
        }
        var worker = new Worker(request.id(), jobsRepository);
        worker.start();
        workers.put(request.id(), worker);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/stop/{id}")
    public ResponseEntity<Void> stopWorker(@PathVariable String id) {
        if (workers.containsKey(id)) {
            workers.remove(id).stop();
        }
        return ResponseEntity.noContent().build();
    }
}
