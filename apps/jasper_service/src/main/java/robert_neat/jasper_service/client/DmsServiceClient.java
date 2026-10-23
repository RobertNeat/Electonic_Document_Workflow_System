package robert_neat.jasper_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import robert_neat.jasper_service.dto.SzablonMetadane;
import robert_neat.jasper_service.exception.BadRequestException;
import robert_neat.jasper_service.exception.NotFoundException;

/** Klient HTTP do dms_service - jedyny sposób dostępu jasper_service do plików szablonów. */
@Service
public class DmsServiceClient {

    private final RestClient restClient;

    public DmsServiceClient(RestClient.Builder restClientBuilder, @Value("${dms.service.base-url}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    public SzablonMetadane pobierzMetadaneSzablonu(String szablonId) {
        try {
            return restClient.get()
                    .uri("/api/szablony/{id}", szablonId)
                    .retrieve()
                    .body(SzablonMetadane.class);
        } catch (RestClientException ex) {
            throw przemapuj(ex, szablonId);
        }
    }

    public byte[] pobierzPlikSzablonu(String szablonId) {
        try {
            return restClient.get()
                    .uri("/api/szablony/{id}/plik", szablonId)
                    .retrieve()
                    .body(byte[].class);
        } catch (RestClientException ex) {
            throw przemapuj(ex, szablonId);
        }
    }

    private RuntimeException przemapuj(RestClientException ex, String szablonId) {
        if (ex instanceof RestClientResponseException responseEx && responseEx.getStatusCode().value() == 404) {
            return new NotFoundException("Szablon o id " + szablonId + " nie istnieje w dms_service");
        }
        return new BadRequestException("Błąd komunikacji z dms_service dla szablonu " + szablonId + ": " + ex.getMessage());
    }
}
