package robert_neat.document_api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.document_api.TestcontainersConfiguration;
import robert_neat.document_api.repository.WpisDziennikaZdarzenRepository;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static robert_neat.document_api.TestJwts.jwtWithRole;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy integracyjne modułu Administracja na realnej bazie Postgres (Testcontainers), z liquibase
 * contextem "demo" (domyślny) dającym seedowane dane: użytkownicy u-1..u-6 z rolami (u-3, u-6 mają
 * rolę kierownik; u-2, u-3, u-5 są w Wydziale Organizacyjnym), wydziały w-1..w-4 ze stanowiskami
 * (w-2 ma 4 stanowiska, pozostałe po 3), pozycje JRWA 1510/3120/6220, typy dokumentów td-1..td-4,
 * dostawcy wysyłki dw-1..dw-3, stawki opłat s-1..s-4, dziennik zdarzeń ev-1..ev-10.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AdministracjaControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WpisDziennikaZdarzenRepository wpisDziennikaZdarzenRepository;

    // --- uzytkownicy ---

    @Test
    void listaUzytkownikow_bezFiltrowZwracaWszystkich() throws Exception {
        mockMvc.perform(get("/api/uzytkownicy").with(jwtWithRole("administrator")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(6)))
                .andExpect(jsonPath("$[?(@.id=='u-1')].login").value(hasItem("demo.referent")));
    }

    @Test
    void listaUzytkownikow_filtrPoWydziale() throws Exception {
        mockMvc.perform(get("/api/uzytkownicy").param("wydzial", "Wydział Organizacyjny").with(jwtWithRole("administrator")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='u-2')]").exists())
                .andExpect(jsonPath("$[?(@.id=='u-3')]").exists())
                .andExpect(jsonPath("$[?(@.id=='u-5')]").exists())
                .andExpect(jsonPath("$[?(@.id=='u-1')]").doesNotExist());
    }

    @Test
    void listaUzytkownikow_filtrPoRoli() throws Exception {
        mockMvc.perform(get("/api/uzytkownicy").param("rola", "KIEROWNIK").with(jwtWithRole("administrator")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='u-3')]").exists())
                .andExpect(jsonPath("$[?(@.id=='u-6')]").exists())
                .andExpect(jsonPath("$[?(@.id=='u-1')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.id=='u-6')].role").value(hasItem(org.hamcrest.Matchers.hasItem("kierownik"))));
    }

    @Test
    @Transactional
    void utworzUzytkownika_sukces201() throws Exception {
        String body = """
                {
                  "login": "nowy.testowy",
                  "imie": "Nowy",
                  "nazwisko": "Testowy",
                  "email": "nowy.testowy@urzad.gov.pl",
                  "role": ["referent"],
                  "wydzial": "Wydział Ochrony Środowiska",
                  "stanowisko": "Referent"
                }
                """;
        mockMvc.perform(post("/api/uzytkownicy")
                        .with(jwtWithRole("administrator")).with(csrf())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.login").value("nowy.testowy"))
                .andExpect(jsonPath("$.role[0]").value("referent"));
    }

    @Test
    @Transactional
    void utworzUzytkownika_duplikatLoginu400() throws Exception {
        String body = """
                {
                  "login": "demo.referent",
                  "imie": "Inny",
                  "nazwisko": "Uzytkownik",
                  "email": "inny@urzad.gov.pl",
                  "role": ["referent"],
                  "wydzial": "Wydział Ochrony Środowiska",
                  "stanowisko": "Referent"
                }
                """;
        mockMvc.perform(post("/api/uzytkownicy")
                        .with(jwtWithRole("administrator")).with(csrf())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.komunikat").exists());
    }

    @Test
    @Transactional
    void aktualizujRole_sukces() throws Exception {
        long przedLiczba = liczbaWpisowTypu("Zmiana danych użytkownika");
        mockMvc.perform(put("/api/uzytkownicy/{id}/role", "u-1")
                        .with(jwtWithRole("administrator")).with(csrf())
                        .contentType("application/json")
                        .content("{\"role\":[\"referent\",\"kierownik\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role.length()").value(2));

        // Dokładnie jeden nowy wpis dziennika zdarzeń typu ZMIANA_DANYCH_UZYTKOWNIKA.
        org.junit.jupiter.api.Assertions.assertEquals(przedLiczba + 1, liczbaWpisowTypu("Zmiana danych użytkownika"));
    }

    @Test
    @Transactional
    void aktualizujRole_404GdyUzytkownikNieIstnieje() throws Exception {
        mockMvc.perform(put("/api/uzytkownicy/{id}/role", "brak-takiego")
                        .with(jwtWithRole("administrator")).with(csrf())
                        .contentType("application/json")
                        .content("{\"role\":[\"referent\"]}"))
                .andExpect(status().isNotFound());
    }

    // --- struktura organizacyjna ---

    @Test
    void strukturaOrganizacyjna_wydzialyZeStanowiskamiZSeeda() throws Exception {
        mockMvc.perform(get("/api/struktura-organizacyjna").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(4)))
                .andExpect(jsonPath("$[?(@.id=='w-2')].stanowiska.length()").value(hasItem(4)))
                .andExpect(jsonPath("$[?(@.id=='w-1')].stanowiska.length()").value(hasItem(3)));
    }

    // --- slowniki: jrwa ---

    @Test
    void jrwa_lista() throws Exception {
        mockMvc.perform(get("/api/slowniki/jrwa").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.symbol=='1510')].haslo").value(hasItem("Skargi i wnioski")));
    }

    @Test
    @Transactional
    void jrwa_aktualizacjaSukces() throws Exception {
        mockMvc.perform(put("/api/slowniki/jrwa/{symbol}", "1510")
                        .with(jwtWithRole("administrator")).with(csrf())
                        .contentType("application/json")
                        .content("{\"symbol\":\"1510\",\"haslo\":\"Zmienione hasło\",\"kategoriaArchiwalna\":\"B10\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.haslo").value("Zmienione hasło"))
                .andExpect(jsonPath("$.kategoriaArchiwalna").value("B10"));
    }

    @Test
    @Transactional
    void jrwa_404GdyNieIstnieje() throws Exception {
        mockMvc.perform(put("/api/slowniki/jrwa/{symbol}", "9999")
                        .with(jwtWithRole("administrator")).with(csrf())
                        .contentType("application/json")
                        .content("{\"symbol\":\"9999\",\"haslo\":\"X\",\"kategoriaArchiwalna\":\"A\"}"))
                .andExpect(status().isNotFound());
    }

    // --- slowniki: typy dokumentow ---

    @Test
    void typyDokumentow_lista() throws Exception {
        mockMvc.perform(get("/api/slowniki/typy-dokumentow").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='td-1')].nazwa").value(hasItem("Pełnomocnictwo")));
    }

    @Test
    @Transactional
    void typyDokumentow_aktualizacjaSukces() throws Exception {
        mockMvc.perform(put("/api/slowniki/typy-dokumentow/{id}", "td-1")
                        .with(jwtWithRole("administrator")).with(csrf())
                        .contentType("application/json")
                        .content("{\"id\":\"td-1\",\"nazwa\":\"Pełnomocnictwo zmienione\",\"opis\":\"Nowy opis\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nazwa").value("Pełnomocnictwo zmienione"));
    }

    @Test
    @Transactional
    void typyDokumentow_404GdyNieIstnieje() throws Exception {
        mockMvc.perform(put("/api/slowniki/typy-dokumentow/{id}", "brak-takiego")
                        .with(jwtWithRole("administrator")).with(csrf())
                        .contentType("application/json")
                        .content("{\"id\":\"brak-takiego\",\"nazwa\":\"X\",\"opis\":null}"))
                .andExpect(status().isNotFound());
    }

    // --- slowniki: dostawcy wysylki ---

    @Test
    void dostawcyWysylki_lista() throws Exception {
        mockMvc.perform(get("/api/slowniki/dostawcy-wysylki").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='dw-1')].typ").value(hasItem("e-doreczenia")));
    }

    @Test
    @Transactional
    void dostawcyWysylki_aktualizacjaSukces() throws Exception {
        String body = """
                {"id":"dw-1","nazwa":"e-Doręczenia zmienione","typ":"e-doreczenia","operatorPocztowy":null,"email":null,"aktywny":false}
                """;
        mockMvc.perform(put("/api/slowniki/dostawcy-wysylki/{id}", "dw-1")
                        .with(jwtWithRole("administrator")).with(csrf())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nazwa").value("e-Doręczenia zmienione"))
                .andExpect(jsonPath("$.aktywny").value(false));
    }

    @Test
    @Transactional
    void dostawcyWysylki_404GdyNieIstnieje() throws Exception {
        String body = """
                {"id":"brak","nazwa":"X","typ":"email","operatorPocztowy":null,"email":null,"aktywny":true}
                """;
        mockMvc.perform(put("/api/slowniki/dostawcy-wysylki/{id}", "brak-takiego")
                        .with(jwtWithRole("administrator")).with(csrf())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isNotFound());
    }

    // --- slowniki: stawki oplat ---

    @Test
    void stawkiOplat_lista() throws Exception {
        mockMvc.perform(get("/api/slowniki/stawki-oplat").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='s-1')].kwota").value(hasItem(17.0)));
    }

    @Test
    @Transactional
    void stawkiOplat_aktualizacjaSukces() throws Exception {
        mockMvc.perform(put("/api/slowniki/stawki-oplat/{id}", "s-1")
                        .with(jwtWithRole("administrator")).with(csrf())
                        .contentType("application/json")
                        .content("{\"id\":\"s-1\",\"nazwa\":\"Opłata zmieniona\",\"kwota\":25}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nazwa").value("Opłata zmieniona"))
                .andExpect(jsonPath("$.kwota").value(25));
    }

    @Test
    @Transactional
    void stawkiOplat_404GdyNieIstnieje() throws Exception {
        mockMvc.perform(put("/api/slowniki/stawki-oplat/{id}", "brak-takiego")
                        .with(jwtWithRole("administrator")).with(csrf())
                        .contentType("application/json")
                        .content("{\"id\":\"brak-takiego\",\"nazwa\":\"X\",\"kwota\":10}"))
                .andExpect(status().isNotFound());
    }

    // --- audit log ---

    @Test
    void auditLog_filtrPoTypOperacji() throws Exception {
        mockMvc.perform(get("/api/audit-log").param("typOperacji", "Rejestracja wpłaty").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='ev-6')]").exists())
                .andExpect(jsonPath("$[?(@.id=='ev-10')]").exists())
                .andExpect(jsonPath("$[?(@.id=='ev-1')]").doesNotExist());
    }

    @Test
    void auditLog_filtrPoZakresieDat() throws Exception {
        mockMvc.perform(get("/api/audit-log")
                        .param("od", "2026-10-21")
                        .param("do", "2026-10-21")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='ev-4')]").exists())
                .andExpect(jsonPath("$[?(@.id=='ev-5')]").exists())
                .andExpect(jsonPath("$[?(@.id=='ev-6')]").exists())
                .andExpect(jsonPath("$[?(@.id=='ev-7')]").exists())
                .andExpect(jsonPath("$[?(@.id=='ev-3')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.id=='ev-8')]").doesNotExist());
    }

    /** Liczba wpisów dziennika zdarzeń danego typu (wartość DB, np. "Rejestracja wpłaty") - do
     * asercji na przyrost przy znanej liczbie wpisów seedowych tego typu. */
    private long liczbaWpisowTypu(String typOperacjiDbValue) {
        return wpisDziennikaZdarzenRepository.findAll().stream()
                .filter(w -> typOperacjiDbValue.equals(w.getTypOperacji()))
                .count();
    }
}
