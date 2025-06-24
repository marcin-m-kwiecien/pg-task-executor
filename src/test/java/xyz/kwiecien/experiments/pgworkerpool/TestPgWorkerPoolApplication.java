package xyz.kwiecien.experiments.pgworkerpool;

import org.springframework.boot.SpringApplication;

public class TestPgWorkerPoolApplication {

    public static void main(String[] args) {
        SpringApplication.from(PgWorkerPoolApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
