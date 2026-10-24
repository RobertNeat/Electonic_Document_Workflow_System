package robert_neat.sign_service.revocation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * RestClient.Builder jako bean niezalezny, aby OcspClient/CrlClient mogly byc testowane przez
 * MockRestServiceServer bez otwierania realnego socketu (zob. RevocationMockTransportConfiguration
 * w src/test, ktora w testach zastepuje ten bean mockiem) - wzorzec 1:1 z document_api
 * RestClientConfig. Krotki timeout, bo brak odpowiedzi OCSP/CRL respondera musi szybko
 * skapitulowac do fail-closed, nie wisiec do nieskonczonosci (domyslny timeout JDK jest brak-limitu).
 */
@Configuration
public class RevocationRestClientConfig {

    private static final int TIMEOUT_MILLIS = 3000;

    @Bean
    public RestClient.Builder revocationRestClientBuilder() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(TIMEOUT_MILLIS);
        requestFactory.setReadTimeout(TIMEOUT_MILLIS);
        return RestClient.builder().requestFactory(requestFactory);
    }
}
