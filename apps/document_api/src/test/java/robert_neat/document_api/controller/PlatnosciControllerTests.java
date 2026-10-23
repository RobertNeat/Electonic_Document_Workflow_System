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
import robert_neat.document_api.repository.WpisDziennikaZdarzenRepository;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

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
