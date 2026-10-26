package robert_neat.jasper_service.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import robert_neat.jasper_service.dto.SzablonMetadane;
import robert_neat.jasper_service.exception.BadRequestException;
import robert_neat.jasper_service.exception.NotFoundException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Testy klienta HTTP do dms_service na MockRestServiceServer (bez realnego dms_service w testach). */
class DmsServiceClientTests {

    private static final String BASE_URL = "http://localhost:10633";

    private MockRestServiceServer server;
    private DmsServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new DmsServiceClient(builder, BASE_URL);
    }

    @Test
    void pobierzMetadaneSzablonu_zwracaZmapowaneMetadane() {
        String json = """
                {"id":"sz-1","typDokumentuId":"typ-1","nazwa":"testowy-szablon.jrxml","wersja":2,
                 "dataWgrania":"%s","autor":"autor-test"}
                """.formatted(LocalDate.now());
        server.expect(requestTo(BASE_URL + "/api/szablony/sz-1"))
                .andExpect(method(GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        SzablonMetadane wynik = client.pobierzMetadaneSzablonu("sz-1");

        assertThat(wynik).isEqualTo(new SzablonMetadane("sz-1", "typ-1", "testowy-szablon.jrxml", 2, LocalDate.now(), "autor-test"));
        server.verify();
    }

    @Test
    void pobierzMetadaneSzablonu_404MapowaneNaNotFoundException() {
        server.expect(requestTo(BASE_URL + "/api/szablony/brak"))
                .andExpect(method(GET))
                .andRespond(withStatus(NOT_FOUND).body("{\"komunikat\":\"nie istnieje\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThrows(NotFoundException.class, () -> client.pobierzMetadaneSzablonu("brak"));
        server.verify();
    }

    @Test
    void pobierzMetadaneSzablonu_500MapowaneNaBadRequestException() {
        server.expect(requestTo(BASE_URL + "/api/szablony/zly"))
                .andExpect(method(GET))
                .andRespond(withStatus(BAD_REQUEST).body("{\"komunikat\":\"blad\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThrows(BadRequestException.class, () -> client.pobierzMetadaneSzablonu("zly"));
        server.verify();
    }

    @Test
    void pobierzPlikSzablonu_zwracaBajtyPliku() {
        byte[] oczekiwane = "zawartosc-jrxml".getBytes();
        server.expect(requestTo(BASE_URL + "/api/szablony/sz-1/plik"))
                .andExpect(method(GET))
                .andRespond(withSuccess(oczekiwane, MediaType.APPLICATION_OCTET_STREAM));

        byte[] wynik = client.pobierzPlikSzablonu("sz-1");

        assertArrayEquals(oczekiwane, wynik);
        server.verify();
    }

    @Test
    void pobierzPlikSzablonu_404MapowaneNaNotFoundException() {
        server.expect(requestTo(BASE_URL + "/api/szablony/brak/plik"))
                .andExpect(method(GET))
                .andRespond(withStatus(NOT_FOUND).body("{\"komunikat\":\"nie istnieje\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThrows(NotFoundException.class, () -> client.pobierzPlikSzablonu("brak"));
        server.verify();
    }

    @Test
    void pobierzPlikSzablonu_500MapowaneNaBadRequestException() {
        server.expect(requestTo(BASE_URL + "/api/szablony/zly/plik"))
                .andExpect(method(GET))
                .andRespond(withStatus(BAD_REQUEST).body("{\"komunikat\":\"blad\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThrows(BadRequestException.class, () -> client.pobierzPlikSzablonu("zly"));
        server.verify();
    }
}
