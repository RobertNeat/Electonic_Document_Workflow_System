package robert_neat.document_api.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import robert_neat.document_api.exception.BadRequestException;
import robert_neat.document_api.exception.NotFoundException;

import java.util.Map;

/** Klient HTTP do jasper_service - renderowanie PDF z szablonu Jasper (orkiestracja akcji
 * zbiorowej "generowanie dokumentów", zob. AkcjaZbiorowaProcessingService.wykonajGenerowanieDokumentu).
 * jasper_service nie wymaga uwierzytelnienia (brak Spring Security na jego classpath) - zwykły
 * RestClient bez żadnej warstwy OAuth2 client-credentials. */
@Service
public class JasperServiceClient {

    private final RestClient restClient;

    public JasperServiceClient(RestClient.Builder restClientBuilder, @Value("${jasper.service.base-url}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    /** parametry - płaska mapa klucz->wartość przekazywana do JasperReports jako $P{klucz};
     * null/pusta mapa jest akceptowana przez jasper_service (raport bez parametrów). */
    public byte[] renderuj(String szablonId, Map<String, Object> parametry) {
        try {
            return restClient.post()
                    .uri("/api/render/{szablonId}", szablonId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(parametry)
                    .retrieve()
                    .body(byte[].class);
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            if (status == 404) {
                throw new NotFoundException(
                        "Szablon Jasper o id=" + szablonId + " nie istnieje w jasper_service/dms_service: "
                                + ex.getResponseBodyAsString());
            }
            if (status == 400) {
                throw new BadRequestException(
                        "jasper_service odrzucił żądanie renderowania szablonu " + szablonId + ": "
                                + ex.getResponseBodyAsString());
            }
            throw new RestClientException("Błąd komunikacji z jasper_service (renderowanie): " + ex.getMessage(), ex);
        }
    }
}
