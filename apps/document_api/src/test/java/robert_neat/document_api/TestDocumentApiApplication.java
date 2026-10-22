package robert_neat.document_api;

import org.springframework.boot.SpringApplication;

public class TestDocumentApiApplication {

    public static void main(String[] args) {
        SpringApplication.from(DocumentApiApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
