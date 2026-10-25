package robert_neat.document_api.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import robert_neat.document_api.exception.BadRequestException;
import robert_neat.document_api.exception.NotFoundException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Testy klienta HTTP do jasper_service na MockRestServiceServer (bez realnego jasper_service w testach). */
class JasperServiceClientTests {

    private static final String BASE_URL = "http://localhost:10622";

    private MockRestServiceServer server;
    private JasperServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new JasperServiceClient(builder, BASE_URL);
    }

    @Test
    void renderuj_zwracaBajtyPdf() {
        byte[] oczekiwane = "zawartosc-pdf".getBytes();
        server.expect(requestTo(BASE_URL + "/api/render/sz-1"))
                .andExpect(method(POST))
                .andRespond(withSuccess(oczekiwane, MediaType.APPLICATION_PDF));

        byte[] wynik = client.renderuj("sz-1", Map.of("klucz", "wartosc"));

        assertArrayEquals(oczekiwane, wynik);
        server.verify();
    }

    @Test
    void renderuj_404MapowaneNaNotFoundException() {
        server.expect(requestTo(BASE_URL + "/api/render/sz-brak"))
                .andExpect(method(POST))
                .andRespond(withStatus(NOT_FOUND).body("{\"komunikat\":\"nie istnieje\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThrows(NotFoundException.class, () -> client.renderuj("sz-brak", Map.of()));
        server.verify();
    }

    @Test
    void renderuj_400MapowaneNaBadRequestException() {
        server.expect(requestTo(BASE_URL + "/api/render/sz-zly"))
                .andExpect(method(POST))
                .andRespond(withStatus(BAD_REQUEST).body("{\"komunikat\":\"brakujacy parametr\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThrows(BadRequestException.class, () -> client.renderuj("sz-zly", Map.of()));
        server.verify();
    }
}
