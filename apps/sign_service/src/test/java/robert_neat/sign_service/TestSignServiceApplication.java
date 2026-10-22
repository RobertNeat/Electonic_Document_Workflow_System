package robert_neat.sign_service;

import org.springframework.boot.SpringApplication;

public class TestSignServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(SignServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
