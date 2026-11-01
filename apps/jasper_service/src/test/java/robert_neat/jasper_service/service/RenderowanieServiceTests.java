package robert_neat.jasper_service.service;

import org.junit.jupiter.api.Test;
import robert_neat.jasper_service.client.DmsServiceClient;
import robert_neat.jasper_service.dto.SzablonMetadane;
import robert_neat.jasper_service.exception.BadRequestException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Testy jednostkowe RenderowanieService - czysty Mockito, bez kontekstu Springa. */
class RenderowanieServiceTests {

    private static byte[] wczytajTestowyJrxml() throws IOException {
        return wczytajZasob("testowy-szablon.jrxml");
    }

    private static byte[] wczytajZasob(String nazwa) throws IOException {
        try (InputStream in = RenderowanieServiceTests.class.getClassLoader()
                .getResourceAsStream(nazwa)) {
            return in.readAllBytes();
        }
    }

    /** Odwzorowuje JasperServiceClient.budujParametryRaportu w document_api - płaska mapa z polami
     * sprawy/obywatela, część wartości null (osoba fizyczna nie ma NIP, podmiot nie ma PESEL/imienia). */
    private static Map<String, Object> parametrySprawyDlaOsobyFizycznej() {
        Map<String, Object> parametry = new HashMap<>();
        parametry.put("znakSprawy", "WO.6220.14.2026");
        parametry.put("tytulSprawy", "Wniosek o wydanie decyzji środowiskowej");
        parametry.put("wydzial", "Wydział Ochrony Środowiska");
        parametry.put("referent", "Katarzyna Zielińska");
        parametry.put("dataWszczecia", "2026-09-01");
        parametry.put("terminUstawowy", "2026-12-01");
        parametry.put("obywatelImie", "Jan");
        parametry.put("obywatelNazwisko", "Kowalski");
        parametry.put("obywatelNazwaPodmiotu", null);
        parametry.put("obywatelPesel", "80010112345");
        parametry.put("obywatelNip", null);
        parametry.put("obywatelEmail", "jan.kowalski@example.pl");
        parametry.put("obywatelTelefon", "500100200");
        return parametry;
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
    void renderujPdfRenderujeSeedowanyDecyzjaSrodowiskowaZPelnymiParametrami() throws Exception {
        DmsServiceClient dmsServiceClient = mock(DmsServiceClient.class);
        when(dmsServiceClient.pobierzMetadaneSzablonu("sz-1")).thenReturn(metadane("sz-1"));
        when(dmsServiceClient.pobierzPlikSzablonu("sz-1")).thenReturn(wczytajZasob("decyzja_srodowiskowa_v3.jrxml"));
        RenderowanieService service = new RenderowanieService(dmsServiceClient);

        RenderowanieService.WynikRenderowania wynik =
                service.renderujPdf("sz-1", parametrySprawyDlaOsobyFizycznej());

        assertThat(wynik.pdf()).isNotEmpty();
        assertThat(new String(wynik.pdf(), 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    @Test
    void renderujPdfRenderujeSeedowaneWezwanieDoZaplatyZPelnymiParametrami() throws Exception {
        DmsServiceClient dmsServiceClient = mock(DmsServiceClient.class);
        when(dmsServiceClient.pobierzMetadaneSzablonu("sz-2")).thenReturn(metadane("sz-2"));
        when(dmsServiceClient.pobierzPlikSzablonu("sz-2")).thenReturn(wczytajZasob("wezwanie_do_zaplaty_v1.jrxml"));
        RenderowanieService service = new RenderowanieService(dmsServiceClient);

        RenderowanieService.WynikRenderowania wynik =
                service.renderujPdf("sz-2", parametrySprawyDlaOsobyFizycznej());

        assertThat(wynik.pdf()).isNotEmpty();
        assertThat(new String(wynik.pdf(), 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
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
