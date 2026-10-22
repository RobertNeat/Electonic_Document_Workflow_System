package robert_neat.document_api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
                .withInitScript("db/test-init.sql");
    }

    /** Realny broker RabbitMQ (testcontainers-rabbitmq) dla testów akcji zbiorowych - @ServiceConnection
     * nadpisuje spring.rabbitmq.* na dane kontenera, więc wszystkie testy (nie tylko akcje zbiorowe)
     * łączą się z tym kontenerem, a nie z lokalnym localhost:5672. */
    @Bean
    @ServiceConnection
    RabbitMQContainer rabbitMqContainer() {
        return new RabbitMQContainer(DockerImageName.parse("rabbitmq:4-management-alpine"));
    }

}
