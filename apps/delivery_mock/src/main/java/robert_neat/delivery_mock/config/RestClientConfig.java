package robert_neat.delivery_mock.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Zapewnia builder RestClient jawnie, niezależnie od auto-konfiguracji Boota - wzorzec 1:1
 * z jasper_service.config.RestClientConfig. */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
