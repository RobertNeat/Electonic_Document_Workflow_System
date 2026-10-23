package robert_neat.document_api.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import robert_neat.document_api.exception.NotFoundException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/** Testy klienta HTTP do dms_service na MockRestServiceServer (bez realnego dms_service w testach). */
class DmsServiceClientTests {

    private static final String BASE_URL = "http://localhost:10621";

    private MockRestServiceServer server;
    private DmsServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new DmsServiceClient(builder, BASE_URL);
    }

    @Test
    void pobierzPlikWersji_zwracaBajty() {
        byte[] oczekiwane = "zawartosc-pdf".getBytes();
        server.expect(requestTo(BASE_URL + "/api/dokumenty/d-1/wersje/1/plik"))
                .andExpect(method(GET))
                .andRespond(withSuccess(oczekiwane, MediaType.APPLICATION_PDF));

        byte[] wynik = client.pobierzPlikWersji("d-1", 1);

        assertArrayEquals(oczekiwane, wynik);
        server.verify();
    }

    @Test
    void pobierzPlikWersji_404MapowaneNaNotFoundException() {
        server.expect(requestTo(BASE_URL + "/api/dokumenty/d-1/wersje/9/plik"))
                .andExpect(method(GET))
                .andRespond(withStatus(NOT_FOUND).body("{\"komunikat\":\"nie istnieje\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThrows(NotFoundException.class, () -> client.pobierzPlikWersji("d-1", 9));
        server.verify();
    }

    @Test
    void uploadNowaWersja_zwracaNumerWersjiZOdpowiedzi() {
        server.expect(requestTo(BASE_URL + "/api/dokumenty/d-1/wersje"))
                .andExpect(method(POST))
                .andRespond(withSuccess("""
                        {
                          "id": "wd-nowa",
                          "dokumentId": "d-1",
                          "wersja": 2,
                          "podpisana": true,
                          "dataUtworzenia": "2026-10-24",
                          "autor": "Test Autor",
                          "statusPodpisu": "brak"
                        }
                        """, MediaType.APPLICATION_JSON));

        int wersja = client.uploadNowaWersja("d-1", "Test Autor", "podpisany-pdf".getBytes(), "dok.pdf");

        assertEquals(2, wersja);
        server.verify();
    }
}
