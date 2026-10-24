package robert_neat.sign_service;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Zastepuje prawdziwy RestClient.Builder (RevocationRestClientConfig) builderem powiazanym z
 * MockRestServiceServer, aby ZADEN test nie otworzyl realnego socketu do adresu AIA/CDP
 * zapisanego w certyfikatach leaf (sign.pki.public-base-url) - zob. OcspClient/CrlClient, ktore
 * buduja RestClient z wstrzykniętego RestClient.Builder w konstruktorze: bindowanie mocka MUSI
 * nastapic PRZED zbudowaniem tego RestClient (czyli w samej metodzie @Bean), inaczej bind nie ma
 * zadnego efektu. @Primary wygrywa z beanem produkcyjnym tego samego typu dla kazdego testu, ktory
 * @Import-uje te klase. Testy korzystajace z tego mocka musza wywolac reset() i ponownie
 * zaarmowac domyslne stuby w swoim @BeforeEach (zob. RevocationStubs).
 */
@TestConfiguration(proxyBeanMethods = false)
public class RevocationMockTransportConfiguration {

    public record MockRevocationTransport(RestClient.Builder builder, MockRestServiceServer server) {
    }

    @Bean
    public MockRevocationTransport mockRevocationTransport() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        return new MockRevocationTransport(builder, server);
    }

    @Bean
    @Primary
    public RestClient.Builder mockRevocationRestClientBuilder(MockRevocationTransport mockRevocationTransport) {
        return mockRevocationTransport.builder();
    }
}
