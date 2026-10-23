package robert_neat.delivery_mock.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Klient HTTP do document_api - delivery_mock jest KONSUMENTEM `GET /api/przesylki` i
 * `POST /api/przesylki/{id}/status` (zob. PrzesylkaController/AktualizujStatusPrzesylkiRequest
 * w document_api). document_api nie wystawia filtra po numerReferencyjny, więc pobieramy pełną
 * listę i dopasowujemy po stronie klienta - zaakceptowane ograniczenie dla małej skali demo.
 * document_api wymaga tokenu Keycloak (OAuth2 Resource Server) - ten klient pozyskuje go flow
 * client-credentials (rejestracja "document-api", zob. application.properties) przez
 * OAuth2AuthorizedClientManager, który sam cache'uje/odnawia token do wygaśnięcia. Błędy
 * komunikacji (sieciowe, HTTP, jak i pozyskania tokenu) NIE są mapowane na wyjątki domenowe
 * delivery_mock (NotFound/BadRequest) - ten klient jest wołany z kontekstu, w którym brak/awaria
 * document_api nie powinna przerywać lokalnej zmiany statusu (zob. SymulacjaDostawyService) -
 * stąd zwykłe RestClientException/OAuth2AuthorizationException (RuntimeException) propagowane
 * do wywołującego, który je łapie. */
@Service
public class DocumentApiClient {

    private static final String REJESTRACJA_DOCUMENT_API = "document-api";

    /** Principal techniczny client-credentials - manager go wymaga, ale przy tym typie grantu
     * nie odpowiada żadnemu zalogowanemu użytkownikowi (brak sesji OAuth2 w delivery_mock). */
    private static final String PRINCIPAL_SERWISOWY = "delivery-mock";

    private final RestClient restClient;
    private final OAuth2AuthorizedClientManager authorizedClientManager;

    public DocumentApiClient(RestClient.Builder restClientBuilder, @Value("${document.api.base-url}") String baseUrl,
                              OAuth2AuthorizedClientManager authorizedClientManager) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.authorizedClientManager = authorizedClientManager;
    }

    /** Pozyskuje (albo odświeża z cache managera) token client-credentials i zwraca go do
     * dołączenia jako nagłówek Authorization. */
    private String pobierzToken() {
        OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest.withClientRegistrationId(REJESTRACJA_DOCUMENT_API)
                .principal(PRINCIPAL_SERWISOWY)
                .build();
        OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(request);
        if (authorizedClient == null) {
            throw new IllegalStateException("Nie udało się autoryzować klienta OAuth2 '" + REJESTRACJA_DOCUMENT_API + "'");
        }
        OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
        return accessToken.getTokenValue();
    }

    /** Odnajduje przesyłkę w document_api po numerze referencyjnym (pole łączące obie strony). */
    public Optional<String> znajdzIdPoNumerzeReferencyjnym(String numerReferencyjny) {
        List<PrzesylkaDocumentApi> wszystkie = restClient.get()
                .uri("/api/przesylki")
                .headers(headers -> headers.setBearerAuth(pobierzToken()))
                .retrieve()
                .body(new ParameterizedTypeReference<List<PrzesylkaDocumentApi>>() {
                });
        if (wszystkie == null) {
            return Optional.empty();
        }
        return wszystkie.stream()
                .filter(p -> numerReferencyjny.equals(p.numerReferencyjny()))
                .map(PrzesylkaDocumentApi::id)
                .findFirst();
    }

    /** Wywołuje synchroniczną aktualizację statusu po stronie document_api. Rzuca
     * RestClientException przy błędzie sieciowym/HTTP - wywołujący decyduje, czy to fatalne. */
    public void aktualizujStatus(String przesylkaId, String nowyStatus) {
        restClient.post()
                .uri("/api/przesylki/{id}/status", przesylkaId)
                .headers(headers -> headers.setBearerAuth(pobierzToken()))
                .body(Map.of("nowyStatus", nowyStatus))
                .retrieve()
                .toBodilessEntity();
    }

    /** Fragment DTO `Przesylka` document_api - tylko pola potrzebne do dopasowania po numerze
     * referencyjnym; ignoruje pozostałe (historiaStatusow, kanal itd.), bo RestClient.builder()
     * tutaj nie korzysta z tunningu Boota dla ObjectMappera. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PrzesylkaDocumentApi(String id, String numerReferencyjny) {
    }
}
