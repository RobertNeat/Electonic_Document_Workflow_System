package robert_neat.jasper_service.service;

import org.junit.jupiter.api.Test;
import robert_neat.jasper_service.client.DmsServiceClient;
import robert_neat.jasper_service.dto.SzablonMetadane;
import robert_neat.jasper_service.exception.BadRequestException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Testy jednostkowe RenderowanieService - czysty Mockito, bez kontekstu Springa. */
class RenderowanieServiceTests {

    private static byte[] wczytajTestowyJrxml() throws IOException {
        try (InputStream in = RenderowanieServiceTests.class.getClassLoader()
                .getResourceAsStream("testowy-szablon.jrxml")) {
            return in.readAllBytes();
        }
    }

    private static SzablonMetadane metadane(String id) {
        return new SzablonMetadane(id, "typ-1", "testowy-szablon.jrxml", 1, LocalDate.now(), "autor-test");
    }

    @Test
    void renderujPdfZwracaPdfDlaPoprawnegoSzablonu() throws Exception {
        DmsServiceClient dmsServiceClient = mock(DmsServiceClient.class);
        when(dmsServiceClient.pobierzMetadaneSzablonu("szablon-1")).thenReturn(metadane("szablon-1"));
        when(dmsServiceClient.pobierzPlikSzablonu("szablon-1")).thenReturn(wczytajTestowyJrxml());
        RenderowanieService service = new RenderowanieService(dmsServiceClient);

        RenderowanieService.WynikRenderowania wynik = service.renderujPdf("szablon-1", Map.of("tytul", "Testowy tytul"));

        assertThat(wynik.pdf()).isNotEmpty();
        assertThat(new String(wynik.pdf(), 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        assertThat(wynik.nazwaSzablonu()).isEqualTo("testowy-szablon.jrxml");
    }

    @Test
    void renderujPdfRzucaBadRequestDlaUszkodzonegoJrxml() {
        DmsServiceClient dmsServiceClient = mock(DmsServiceClient.class);
        when(dmsServiceClient.pobierzMetadaneSzablonu("uszkodzony")).thenReturn(metadane("uszkodzony"));
        when(dmsServiceClient.pobierzPlikSzablonu("uszkodzony"))
                .thenReturn("to nie jest poprawny jrxml".getBytes(StandardCharsets.UTF_8));
        RenderowanieService service = new RenderowanieService(dmsServiceClient);

        assertThatThrownBy(() -> service.renderujPdf("uszkodzony", Map.of()))
                .isInstanceOf(BadRequestException.class);
    }
}
