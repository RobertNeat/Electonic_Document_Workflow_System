package robert_neat.dms_service;

import org.springframework.boot.SpringApplication;

public class TestDmsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(DmsServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
