package robert_neat.document_api.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import robert_neat.document_api.exception.BadRequestException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Testy klienta HTTP do sign_service na MockRestServiceServer (bez realnego sign_service w testach). */
class SignServiceClientTests {

    private static final String BASE_URL = "http://localhost:10623";

    private MockRestServiceServer server;
    private SignServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new SignServiceClient(builder, BASE_URL);
    }

    @Test
    void podpiszPades_zwracaPodpisanyPlikIIdPodpisu() {
        byte[] podpisanyPlik = "podpisany-pdf".getBytes();
        server.expect(requestTo(BASE_URL + "/api/podpisy/pades"))
                .andExpect(method(POST))
                .andRespond(withSuccess(podpisanyPlik, MediaType.APPLICATION_PDF)
                        .header("X-Wystawiony-Podpis-Id", "podpis-123"));

        SignServiceClient.WynikPodpisaniaPades wynik =
                client.podpiszPades("do-podpisu".getBytes(), "cert-1", "wd-1");

        assertArrayEquals(podpisanyPlik, wynik.podpisanyPlik());
        assertEquals("podpis-123", wynik.wystawionyPodpisId());
        server.verify();
    }

    @Test
    void podpiszPades_400MapowaneNaBadRequestException() {
        server.expect(requestTo(BASE_URL + "/api/podpisy/pades"))
                .andExpect(method(POST))
                .andRespond(withStatus(BAD_REQUEST).body("{\"komunikat\":\"certyfikat wygasły\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThrows(BadRequestException.class,
                () -> client.podpiszPades("do-podpisu".getBytes(), "cert-wygasly", "wd-1"));
        server.verify();
    }

    @Test
    void podpiszPades_404MapowaneNaBadRequestException() {
        server.expect(requestTo(BASE_URL + "/api/podpisy/pades"))
                .andExpect(method(POST))
                .andRespond(withStatus(NOT_FOUND).body("{\"komunikat\":\"nie istnieje\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThrows(BadRequestException.class,
                () -> client.podpiszPades("do-podpisu".getBytes(), "brak-takiego", "wd-1"));
        server.verify();
    }
}
