package robert_neat.document_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Zapewnia builder RestClient jawnie, niezależnie od auto-konfiguracji Boota - wzorzec 1:1
 * z jasper_service/delivery_mock. dms_service i sign_service nie wymagają uwierzytelnienia
 * (brak Spring Security na ich classpath), więc klienci poniżej nie potrzebują OAuth2
 * client-credentials - zwykły RestClient jest kompletnym rozwiązaniem. */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
