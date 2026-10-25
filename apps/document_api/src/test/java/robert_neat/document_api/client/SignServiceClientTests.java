package robert_neat.document_api.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import robert_neat.document_api.dto.dokumenty.Certyfikat;
import robert_neat.document_api.exception.BadRequestException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
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

    @Test
    void listaCertyfikatow_zwracaListeCertyfikatow() {
        String json = """
                [
                  {"id":"cert-1","podmiot":"CN=Katarzyna Zielińska, O=Urząd, C=PL",
                   "numerSeryjny":"7A3F1B2C9D0E4455","dataWydania":"2026-01-10",
                   "dataWygasniecia":"2027-01-10","status":"aktywny","wydawca":"Demo CA Issuing"}
                ]
                """;
        server.expect(requestTo(BASE_URL + "/api/certyfikaty"))
                .andExpect(method(GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<Certyfikat> wynik = client.listaCertyfikatow();

        assertEquals(1, wynik.size());
        assertEquals("cert-1", wynik.get(0).id());
        assertEquals("aktywny", wynik.get(0).status());
        server.verify();
    }

    @Test
    void listaCertyfikatow_bladKomunikacjiMapowanyNaRestClientException() {
        server.expect(requestTo(BASE_URL + "/api/certyfikaty"))
                .andExpect(method(GET))
                .andRespond(withStatus(INTERNAL_SERVER_ERROR));

        assertThrows(RestClientException.class, client::listaCertyfikatow);
        server.verify();
    }
}
