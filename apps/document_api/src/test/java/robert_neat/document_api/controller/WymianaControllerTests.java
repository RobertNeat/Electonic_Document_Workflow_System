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
import robert_neat.document_api.repository.NaleznoscRepository;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static robert_neat.document_api.TestJwts.jwtWithRole;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy integracyjne modułu Wymiana na realnej bazie Postgres (Testcontainers), z liquibase
 * contextem "demo" dającym seedowane wpisy historii: hx-1..hx-4 (typ xml), hu-1..hu-3 (typ ubl).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class WymianaControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NaleznoscRepository naleznoscRepository;

    // --- historia ---

    @Test
    void historiaXml_zwracaWpisySeedowe() throws Exception {
        mockMvc.perform(get("/api/wymiana/xml/historia").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(4)))
                .andExpect(jsonPath("$[?(@.id=='hx-1')].nazwaPliku").value(hasItem("komunikat_sprawa_WO.6220.14.2026.xml")))
                .andExpect(jsonPath("$[?(@.id=='hx-3')].status").value(hasItem("blad_walidacji")));
    }

    @Test
    void historiaUbl_zwracaWpisySeedowe() throws Exception {
        mockMvc.perform(get("/api/wymiana/ubl/historia").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$[?(@.id=='hu-1')].nazwaPliku").value(hasItem("faktura_FK-2026-0088.xml")))
                .andExpect(jsonPath("$[?(@.id=='hu-3')].status").value(hasItem("blad_walidacji")));
    }

    // --- import XML (komunikat sprawy) ---

    @Test
    @Transactional
    void importXml_poprawnyPlikMapujeSprawe() throws Exception {
        byte[] xml = getClass().getResourceAsStream("/wymiana/komunikat-sprawy-poprawny.xml").readAllBytes();
        MockMultipartFile plik = new MockMultipartFile("plik", "komunikat-sprawy-poprawny.xml", "application/xml", xml);

        mockMvc.perform(multipart("/api/wymiana/xml").file(plik).with(jwtWithRole("referent")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sukces").value(true))
                .andExpect(jsonPath("$.bledy.length()").value(0))
                .andExpect(jsonPath("$.sprawaZmapowana.znakSprawy").value("WO.6220.99.2026"))
                .andExpect(jsonPath("$.sprawaZmapowana.tytul").value("Decyzja środowiskowa dla inwestycji drogowej"))
                .andExpect(jsonPath("$.sprawaZmapowana.obywatelId").value("ob-1"));

        mockMvc.perform(get("/api/wymiana/xml/historia").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nazwaPliku=='komunikat-sprawy-poprawny.xml')].status").value(hasItem("sukces")));
    }

    @Test
    @Transactional
    void importXml_niepoprawnyPlikZwracaBledy() throws Exception {
        byte[] xml = getClass().getResourceAsStream("/wymiana/komunikat-sprawy-niepoprawny.xml").readAllBytes();
        MockMultipartFile plik = new MockMultipartFile("plik", "komunikat-sprawy-niepoprawny.xml", "application/xml", xml);

        mockMvc.perform(multipart("/api/wymiana/xml").file(plik).with(jwtWithRole("referent")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sukces").value(false))
                .andExpect(jsonPath("$.bledy[?(@.element=='znakSprawy')]").exists());

        mockMvc.perform(get("/api/wymiana/xml/historia").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nazwaPliku=='komunikat-sprawy-niepoprawny.xml')].status").value(hasItem("blad_walidacji")));
    }

    @Test
    @Transactional
    void importXml_400GdyPlikNieJestXml() throws Exception {
        byte[] tresc = "nie xml".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile plik = new MockMultipartFile("plik", "nie-xml.txt", "text/plain", tresc);

        mockMvc.perform(multipart("/api/wymiana/xml").file(plik).with(jwtWithRole("referent")).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.komunikat").exists());
    }

    // --- import UBL (faktura) ---

    @Test
    @Transactional
    void importUbl_poprawnyPlikMapujeFaktureBezZapisuNaleznosci() throws Exception {
        long liczbaNaleznosciPrzed = naleznoscRepository.count();

        byte[] xml = getClass().getResourceAsStream("/wymiana/faktura-ubl-poprawna.xml").readAllBytes();
        MockMultipartFile plik = new MockMultipartFile("plik", "faktura-ubl-poprawna.xml", "application/xml", xml);

        mockMvc.perform(multipart("/api/wymiana/ubl").file(plik).with(jwtWithRole("ksiegowosc")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sukces").value(true))
                .andExpect(jsonPath("$.bledy.length()").value(0))
                .andExpect(jsonPath("$.faktura.numer").value("FK/2026/0099"))
                .andExpect(jsonPath("$.faktura.kontrahent").value("Budimex Serwis Sp. z o.o."))
                .andExpect(jsonPath("$.faktura.nip").value("5260255999"))
                .andExpect(jsonPath("$.faktura.kwota").value(4500))
                .andExpect(jsonPath("$.faktura.pozycje.length()").value(1))
                .andExpect(jsonPath("$.faktura.pozycje[0].nazwa").value("Usługa serwisowa - konserwacja urządzeń"))
                .andExpect(jsonPath("$.faktura.numerReferencyjnyNaleznosci").value("FKI/FK20260099"));

        // Import UBL jest wyłącznie podglądowy - żadna Naleznosc nie jest tworzona.
        org.junit.jupiter.api.Assertions.assertEquals(liczbaNaleznosciPrzed, naleznoscRepository.count());

        mockMvc.perform(get("/api/wymiana/ubl/historia").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nazwaPliku=='faktura-ubl-poprawna.xml')].status").value(hasItem("sukces")));
    }

    @Test
    @Transactional
    void importUbl_niepoprawnyNamespaceZwracaBlad() throws Exception {
        byte[] xml = getClass().getResourceAsStream("/wymiana/faktura-ubl-niepoprawny-namespace.xml").readAllBytes();
        MockMultipartFile plik = new MockMultipartFile("plik", "faktura-ubl-niepoprawny-namespace.xml", "application/xml", xml);

        mockMvc.perform(multipart("/api/wymiana/ubl").file(plik).with(jwtWithRole("ksiegowosc")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sukces").value(false))
                .andExpect(jsonPath("$.faktura").doesNotExist())
                .andExpect(jsonPath("$.bledy[0].element").value("Invoice"));

        mockMvc.perform(get("/api/wymiana/ubl/historia").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nazwaPliku=='faktura-ubl-niepoprawny-namespace.xml')].status").value(hasItem("blad_walidacji")));
    }

    @Test
    @Transactional
    void importUbl_brakWymaganychPolZwracaBledyPolaKazdyOsobno() throws Exception {
        byte[] xml = getClass().getResourceAsStream("/wymiana/faktura-ubl-brak-pol.xml").readAllBytes();
        MockMultipartFile plik = new MockMultipartFile("plik", "faktura-ubl-brak-pol.xml", "application/xml", xml);

        mockMvc.perform(multipart("/api/wymiana/ubl").file(plik).with(jwtWithRole("ksiegowosc")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sukces").value(false))
                .andExpect(jsonPath("$.faktura").doesNotExist())
                .andExpect(jsonPath("$.bledy[?(@.element=='cac:PartyTaxScheme/cbc:CompanyID')]").exists())
                .andExpect(jsonPath("$.bledy[?(@.element=='cbc:PayableAmount')]").exists());

        mockMvc.perform(get("/api/wymiana/ubl/historia").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nazwaPliku=='faktura-ubl-brak-pol.xml')].status").value(hasItem("blad_walidacji")));
    }

    @Test
    @Transactional
    void importUbl_400GdyPlikNieJestXml() throws Exception {
        byte[] tresc = "nie xml".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile plik = new MockMultipartFile("plik", "nie-xml.txt", "text/plain", tresc);

        mockMvc.perform(multipart("/api/wymiana/ubl").file(plik).with(jwtWithRole("ksiegowosc")).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.komunikat").exists());
    }
}
