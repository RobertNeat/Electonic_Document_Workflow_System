package robert_neat.delivery_mock;

import org.springframework.boot.SpringApplication;

public class TestDeliveryMockApplication {

    public static void main(String[] args) {
        SpringApplication.from(DeliveryMockApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
