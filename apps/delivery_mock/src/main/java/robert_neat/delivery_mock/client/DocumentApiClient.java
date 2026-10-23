package robert_neat.delivery_mock.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Klient HTTP do document_api - delivery_mock jest KONSUMENTEM `GET /api/przesylki` i
 * `POST /api/przesylki/{id}/status` (zob. PrzesylkaController/AktualizujStatusPrzesylkiRequest
 * w document_api). document_api nie wystawia filtra po numerReferencyjny, więc pobieramy pełną
 * listę i dopasowujemy po stronie klienta - zaakceptowane ograniczenie dla małej skali demo.
 * Błędy komunikacji NIE są mapowane na wyjątki domenowe delivery_mock (NotFound/BadRequest) -
 * ten klient jest wołany z kontekstu, w którym brak/awaria document_api nie powinna przerywać
 * lokalnej zmiany statusu (zob. SymulacjaDostawyService) - stąd zwykłe RestClientException
 * (albo RuntimeException z deserializacji) propagowane do wywołującego, który je łapie. */
@Service
public class DocumentApiClient {

    private final RestClient restClient;

    public DocumentApiClient(RestClient.Builder restClientBuilder, @Value("${document.api.base-url}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    /** Odnajduje przesyłkę w document_api po numerze referencyjnym (pole łączące obie strony). */
    public Optional<String> znajdzIdPoNumerzeReferencyjnym(String numerReferencyjny) {
        List<PrzesylkaDocumentApi> wszystkie = restClient.get()
                .uri("/api/przesylki")
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
