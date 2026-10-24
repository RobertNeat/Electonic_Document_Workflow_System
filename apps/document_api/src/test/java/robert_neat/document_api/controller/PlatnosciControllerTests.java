package robert_neat.document_api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.document_api.TestcontainersConfiguration;
import robert_neat.document_api.entity.WplataEntity;
import robert_neat.document_api.repository.WpisDziennikaZdarzenRepository;
import robert_neat.document_api.repository.WplataRepository;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static robert_neat.document_api.TestJwts.jwtWithRole;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy integracyjne modułu Płatności na realnej bazie Postgres (Testcontainers), z liquibase
 * contextem "demo" dającym seedowane dane: należności n-1 (spr-1/ob-1, rozliczona, 17 PLN,
 * numer OPL/2026/5541) i n-2 (spr-3/ob-3, nierozliczona, 4500 PLN, numer FK/2026/0088),
 * wpłaty w-1 (dopasowana do n-1) i w-2 (niedopasowana, 250 PLN).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PlatnosciControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WpisDziennikaZdarzenRepository wpisDziennikaZdarzenRepository;

    @Autowired
    private WplataRepository wplataRepository;

    // --- naleznosci: listowanie/szczegoly ---

    @Test
    void listaNaleznosci_bezFiltrowZwracaWszystkieZeZnakiemIObywatelem() throws Exception {
        mockMvc.perform(get("/api/naleznosci").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$[?(@.id=='n-1')].znakSprawy").value(hasItem("WO.6220.14.2026")))
                .andExpect(jsonPath("$[?(@.id=='n-1')].obywatelNazwa").value(hasItem("Jan Kowalski")))
                .andExpect(jsonPath("$[?(@.id=='n-2')].znakSprawy").value(hasItem("FK.3120.88.2026")))
                .andExpect(jsonPath("$[?(@.id=='n-2')].obywatelNazwa").value(hasItem("Budimex Serwis Sp. z o.o.")));
    }

    @Test
    void listaNaleznosci_filtrPoStatusie() throws Exception {
        mockMvc.perform(get("/api/naleznosci").param("status", "nierozliczona").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='n-2')]").exists())
                .andExpect(jsonPath("$[?(@.id=='n-1')]").doesNotExist());
    }

    @Test
    void listaNaleznosci_filtrPoObywatelu() throws Exception {
        mockMvc.perform(get("/api/naleznosci").param("obywatelId", "ob-1").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='n-1')]").exists())
                .andExpect(jsonPath("$[?(@.id=='n-2')]").doesNotExist());
    }

    @Test
    void listaNaleznosci_filtrPoObywatelu_iStatusie_dzialaLacznie() throws Exception {
        // n-1 jest "rozliczona" u ob-1 - filtr obywatelId=ob-1 + status=nierozliczona nie powinien
        // go zwrócić (AND, nie OR).
        mockMvc.perform(get("/api/naleznosci")
                        .param("obywatelId", "ob-1").param("status", "nierozliczona").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='n-1')]").doesNotExist());

        mockMvc.perform(get("/api/naleznosci")
                        .param("obywatelId", "ob-1").param("status", "rozliczona").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='n-1')]").exists());
    }

    @Test
    void listaNaleznosci_filtrPoObywatelu_nieznanyObywatel_zwracaPustaListe() throws Exception {
        mockMvc.perform(get("/api/naleznosci").param("obywatelId", "brak-takiego").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getNaleznoscById_zwracaDaneZSeeda() throws Exception {
        mockMvc.perform(get("/api/naleznosci/n-2").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numerReferencyjny").value("FK/2026/0088"))
                .andExpect(jsonPath("$.kwota").value(4500))
                .andExpect(jsonPath("$.status").value("nierozliczona"));
    }

    @Test
    void getNaleznoscById_404GdyNieIstnieje() throws Exception {
        mockMvc.perform(get("/api/naleznosci/brak-takiej").with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.komunikat").exists());
    }

    // --- wplaty: listowanie ---

    @Test
    void listaWplat_bezFiltruZwracaWszystkie() throws Exception {
        mockMvc.perform(get("/api/wplaty").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$[?(@.id=='w-1')]").exists())
                .andExpect(jsonPath("$[?(@.id=='w-2')]").exists());
    }

    @Test
    void listaWplat_filtrPoNaleznosci() throws Exception {
        mockMvc.perform(get("/api/wplaty").param("naleznoscId", "n-1").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("w-1"));
    }

    @Test
    void listaWplat_filtrPoObywatelu() throws Exception {
        // w-1 dopasowana do n-1 (ob-1) - filtr obywatelId=ob-1 musi go zwrócić.
        mockMvc.perform(get("/api/wplaty").param("obywatelId", "ob-1").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='w-1')]").exists());
    }

    @Test
    void listaWplat_filtrPoNaleznosciIObywatelu_dzialaLacznie() throws Exception {
        mockMvc.perform(get("/api/wplaty")
                        .param("naleznoscId", "n-1").param("obywatelId", "ob-1").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='w-1')]").exists());

        // n-1 jest u ob-1, nie ob-3 - kombinacja naleznoscId=n-1 + obywatelId=ob-3 nie może dać wyniku.
        mockMvc.perform(get("/api/wplaty")
                        .param("naleznoscId", "n-1").param("obywatelId", "ob-3").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='w-1')]").doesNotExist());
    }

    /** Przypadek subtelny: wpłata niedopasowana (naleznosc_id IS NULL) nie może dopasować się do
     * ŻADNEGO filtru obywatelId - ścieżka Criteria na pustej relacji @ManyToOne nie zgłasza
     * wyjątku (nie jest dereferencją Java), tylko poprawnie nie trafia w SQL JOIN. Fixture
     * tworzony explicite (nie licząc na seedowe w-2), żeby test był niezależny od innych klas
     * testowych mogących dopasować w-2 do jakiejś należności. */
    @Test
    @Transactional
    void listaWplat_filtrPoObywatelu_wplataNiedopasowanaNieRzucaWyjatkuIJestWykluczona() throws Exception {
        WplataEntity niedopasowana = new WplataEntity();
        niedopasowana.setId("w-test-niedopasowana");
        niedopasowana.setNaleznosc(null);
        niedopasowana.setKwota(new BigDecimal("123.45"));
        niedopasowana.setData(LocalDate.now());
        niedopasowana.setTytul("Wpłata testowa niedopasowana");
        niedopasowana.setKontrahent("Nieznany");
        niedopasowana.setDopasowanaAutomatycznie(false);
        wplataRepository.saveAndFlush(niedopasowana);

        mockMvc.perform(get("/api/wplaty").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='w-test-niedopasowana')]").exists());

        mockMvc.perform(get("/api/wplaty").param("obywatelId", "ob-1").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='w-test-niedopasowana')]").doesNotExist());

        mockMvc.perform(get("/api/wplaty").param("obywatelId", "ob-3").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='w-test-niedopasowana')]").doesNotExist());
    }

    // --- wplaty: dopasowanie reczne ---

    @Test
    @Transactional
    void dopasujWplate_sukcesCzescioweRozliczenie() throws Exception {
        long przedLiczba = liczbaWpisowTypu("Rejestracja wpłaty");
        // w-2 (250 PLN) dopasowywana do n-2 (4500 PLN, 0 rozliczone) -> czesciowo_rozliczona.
        mockMvc.perform(post("/api/wplaty/{id}/dopasuj", "w-2")
                        .with(jwtWithRole("ksiegowosc")).with(csrf())
                        .contentType("application/json")
                        .content("{\"naleznoscId\":\"n-2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.naleznoscId").value("n-2"));

        mockMvc.perform(get("/api/naleznosci/n-2").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kwotaRozliczona").value(250))
                .andExpect(jsonPath("$.status").value("czesciowo_rozliczona"));

        // Dokładnie jeden nowy wpis dziennika zdarzeń typu REJESTRACJA_WPLATY.
        org.junit.jupiter.api.Assertions.assertEquals(przedLiczba + 1, liczbaWpisowTypu("Rejestracja wpłaty"));
    }

    @Test
    @Transactional
    void dopasujWplate_404GdyWplataNieIstnieje() throws Exception {
        mockMvc.perform(post("/api/wplaty/{id}/dopasuj", "brak-takiej")
                        .with(jwtWithRole("ksiegowosc")).with(csrf())
                        .contentType("application/json")
                        .content("{\"naleznoscId\":\"n-2\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void dopasujWplate_404GdyNaleznoscNieIstnieje() throws Exception {
        mockMvc.perform(post("/api/wplaty/{id}/dopasuj", "w-2")
                        .with(jwtWithRole("ksiegowosc")).with(csrf())
                        .contentType("application/json")
                        .content("{\"naleznoscId\":\"brak-takiej\"}"))
                .andExpect(status().isNotFound());
    }

    // --- import camt053 ---

    @Test
    @Transactional
    void importCamt053_rozpoznajeIDopasowujeAutomatycznie() throws Exception {
        long przedLiczba = liczbaWpisowTypu("Rejestracja wpłaty");
        byte[] xml = getClass().getResourceAsStream("/camt053/wyciag-dopasowanie.xml").readAllBytes();
        MockMultipartFile plik = new MockMultipartFile("plik", "wyciag.xml", "application/xml", xml);

        mockMvc.perform(multipart("/api/import-camt053").file(plik).with(jwtWithRole("ksiegowosc")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liczbaTransakcji").value(2))
                .andExpect(jsonPath("$.liczbaDopasowanychAutomatycznie").value(1))
                .andExpect(jsonPath("$.liczbaDoRekoncyliacji").value(1))
                .andExpect(jsonPath("$.utworzoneWplaty.length()").value(2))
                .andExpect(jsonPath("$.utworzoneWplaty[?(@.kwota==4500)].naleznoscId").value(hasItem("n-2")))
                .andExpect(jsonPath("$.utworzoneWplaty[?(@.kwota==4500)].dopasowanaAutomatycznie").value(hasItem(true)))
                .andExpect(jsonPath("$.utworzoneWplaty[?(@.kwota==999.99)].naleznoscId").value(hasItem((Object) null)))
                .andExpect(jsonPath("$.utworzoneWplaty[?(@.kwota==999.99)].dopasowanaAutomatycznie").value(hasItem(false)));

        mockMvc.perform(get("/api/naleznosci/n-2").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kwotaRozliczona").value(4500))
                .andExpect(jsonPath("$.status").value("rozliczona"));

        // Jeden zbiorczy wpis dziennika zdarzeń typu REJESTRACJA_WPLATY dla całego importu, nie
        // po jednym na każdą zaimportowaną wpłatę.
        org.junit.jupiter.api.Assertions.assertEquals(przedLiczba + 1, liczbaWpisowTypu("Rejestracja wpłaty"));
    }

    @Test
    @Transactional
    void importCamt053_400GdyPlikNieJestXml() throws Exception {
        byte[] tresc = "nie xml".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile plik = new MockMultipartFile("plik", "nie-xml.txt", "text/plain", tresc);

        mockMvc.perform(multipart("/api/import-camt053").file(plik).with(jwtWithRole("ksiegowosc")).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.komunikat").exists());
    }

    /** Liczba wpisów dziennika zdarzeń danego typu (wartość DB, np. "Rejestracja wpłaty") - do
     * asercji na przyrost przy znanej liczbie wpisów seedowych tego typu. */
    private long liczbaWpisowTypu(String typOperacjiDbValue) {
        return wpisDziennikaZdarzenRepository.findAll().stream()
                .filter(w -> typOperacjiDbValue.equals(w.getTypOperacji()))
                .count();
    }
}
