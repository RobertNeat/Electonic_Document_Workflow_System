package robert_neat.document_api.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import robert_neat.document_api.dto.dokumenty.Certyfikat;
import robert_neat.document_api.exception.BadRequestException;

import java.util.List;

/** Klient HTTP do sign_service - składanie podpisu PAdES (orkiestracja podpisania dokumentu,
 * zob. DokumentService.podpiszDokument) oraz odczyt listy certyfikatów (zasilenie cert-pickera
 * frontu, zob. CertyfikatController). XAdES i weryfikacja podpisu są poza zakresem tej integracji
 * (zob. plan.md). sign_service nie wymaga uwierzytelnienia (brak Spring Security na jego
 * classpath) - zwykły RestClient bez żadnej warstwy OAuth2 client-credentials. */
@Service
public class SignServiceClient {

    private static final String HEADER_WYSTAWIONY_PODPIS_ID = "X-Wystawiony-Podpis-Id";

    private final RestClient restClient;

    public SignServiceClient(RestClient.Builder restClientBuilder, @Value("${sign.service.base-url}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    public record WynikPodpisaniaPades(byte[] podpisanyPlik, String wystawionyPodpisId) {
    }

    /** certyfikatId i dokumentReferencja - identycznie jak w kontrakcie sign_service: żadnej
     * próby podpisania, jeśli certyfikat nie jest aktywny/nie wygasł (400 Bad Request, bez
     * utworzenia wiersza wystawiony_podpis), 404 jeśli certyfikat o podanym id nie istnieje. */
    public WynikPodpisaniaPades podpiszPades(byte[] plik, String certyfikatId, String dokumentReferencja) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("plik", new ByteArrayResource(plik) {
            @Override
            public String getFilename() {
                return "dokument.pdf";
            }
        });
        body.add("certyfikatId", certyfikatId);
        body.add("dokumentReferencja", dokumentReferencja);

        try {
            ResponseEntity<byte[]> odpowiedz = restClient.post()
                    .uri("/api/podpisy/pades")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .toEntity(byte[].class);
            String wystawionyPodpisId = odpowiedz.getHeaders().getFirst(HEADER_WYSTAWIONY_PODPIS_ID);
            return new WynikPodpisaniaPades(odpowiedz.getBody(), wystawionyPodpisId);
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            if (status == 400 || status == 404) {
                // Certyfikat nieaktywny/wygasły (400) albo nieznany certyfikatId (404) - to są
                // błędy złego parametru przekazanego przez wołającego document_api, nie awaria
                // infrastruktury - propagowane jako BadRequestException (400 po stronie document_api).
                throw new BadRequestException("sign_service odrzucił żądanie podpisania: " + ex.getMessage());
            }
            throw new RestClientException("Błąd komunikacji z sign_service (podpisanie PAdES): " + ex.getMessage(), ex);
        }
    }

    /** Proxy do GET /api/certyfikaty - wszystkie certyfikaty (aktywne i nieaktywne), bez
     * filtrowania po tożsamości użytkownika (zob. plan.md - brak modelu użytkownik↔certyfikat). */
    public List<Certyfikat> listaCertyfikatow() {
        try {
            return restClient.get()
                    .uri("/api/certyfikaty")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Certyfikat>>() {
                    });
        } catch (RestClientResponseException ex) {
            throw new RestClientException("Błąd komunikacji z sign_service (lista certyfikatów): " + ex.getMessage(), ex);
        }
    }
}
