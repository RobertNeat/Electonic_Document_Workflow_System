package robert_neat.document_api.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import robert_neat.document_api.exception.NotFoundException;

/** Klient HTTP do dms_service - pobranie bajtów istniejącej wersji i zapis nowej wersji
 * (orkiestracja podpisania dokumentu, zob. DokumentService.podpiszDokument). dms_service nie
 * wymaga uwierzytelnienia (brak Spring Security na jego classpath) - zwykły RestClient bez
 * żadnej warstwy OAuth2 client-credentials. */
@Service
public class DmsServiceClient {

    private final RestClient restClient;

    public DmsServiceClient(RestClient.Builder restClientBuilder, @Value("${dms.service.base-url}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    public byte[] pobierzPlikWersji(String dokumentId, int wersja) {
        try {
            return restClient.get()
                    .uri("/api/dokumenty/{dokumentId}/wersje/{wersja}/plik", dokumentId, wersja)
                    .retrieve()
                    .body(byte[].class);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new NotFoundException(
                        "Plik wersji " + wersja + " dokumentu " + dokumentId + " nie istnieje w dms_service");
            }
            throw new RestClientException("Błąd komunikacji z dms_service (pobranie pliku): " + ex.getMessage(), ex);
        }
    }

    /** Nowy wiersz wersji w dms_service - wersja=MAX+1 nadawana przez dms_service samo,
     * nie przez wołającego. Zwraca numer wersji nadany przez dms_service. */
    public int uploadNowaWersja(String dokumentId, String autor, byte[] plik, String nazwaPliku) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("plik", new ByteArrayResource(plik) {
            @Override
            public String getFilename() {
                return nazwaPliku;
            }
        });
        body.add("autor", autor);

        try {
            WersjaDokumentuDms wynik = restClient.post()
                    .uri("/api/dokumenty/{dokumentId}/wersje", dokumentId)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(WersjaDokumentuDms.class);
            return wynik.wersja();
        } catch (RestClientResponseException ex) {
            throw new RestClientException("Błąd komunikacji z dms_service (upload nowej wersji): " + ex.getMessage(), ex);
        }
    }

    /** Minimalny lokalny odpowiednik WersjaDokumentuDms (dms_service) - potrzebny tylko numer
     * wersji nadany przez dms_service, resztę pól document_api utrzymuje sam w core.wersja_dokumentu. */
    private record WersjaDokumentuDms(
            String id,
            String dokumentId,
            int wersja,
            boolean podpisana,
            String dataUtworzenia,
            String autor,
            String statusPodpisu
    ) {
    }
}
