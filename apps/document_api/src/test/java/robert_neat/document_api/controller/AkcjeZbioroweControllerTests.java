package robert_neat.document_api.controller;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import robert_neat.document_api.TestcontainersConfiguration;
import robert_neat.document_api.repository.WpisDziennikaZdarzenRepository;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static robert_neat.document_api.TestJwts.jwtWithRole;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy integracyjne akcji zbiorowych na realnych Postgres + RabbitMQ (Testcontainers). Konsument
 * RabbitMQ pracuje asynchronicznie na wątku kontenera listenera - asercje na efektach przetwarzania
 * muszą więc odpytywać endpoint statusu w pętli (Awaitility) aż do zakonczona, a nie sprawdzać stan
 * natychmiast po POST. Testy NIE są @Transactional (otwarta transakcja testu skrywałaby commity
 * konsumenta / powodowała niewidoczność między wątkami) - każdy test tworzy własne, świeże wiersze
 * (sprawa/użytkownik), żeby nie zepsuć danych seedowych używanych przez inne klasy testowe.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AkcjeZbioroweControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WpisDziennikaZdarzenRepository wpisDziennikaZdarzenRepository;

    private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(15);

    @Test
    void zmianaStanu_sukcesNaLegalnymPrzejsciu() throws Exception {
        String sprawaId = zarejestrujSprawe();
        long przedLiczba = liczbaWpisowTypu("Zmiana stanu sprawy");

        String akcjaId = uruchomAkcje(Map.of(
                "typAkcji", "ZMIANA_STANU",
                "typCelu", "SPRAWA",
                "idCelow", List.of(sprawaId),
                "parametry", Map.of("nowyStan", "W toku")
        ));

        czekajNaZakonczenie(akcjaId)
                .andExpect(jsonPath("$.przetworzone").value(1))
                .andExpect(jsonPath("$.bledne").value(0))
                .andExpect(jsonPath("$.lacznie").value(1));

        mockMvc.perform(get("/api/sprawy/{id}", sprawaId).with(jwtWithRole("referent")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stan").value("W toku"));

        // Dokładnie jeden nowy wpis dziennika zdarzeń typu ZMIANA_STANU_SPRAWY z przetwarzania akcji zbiorowej.
        org.junit.jupiter.api.Assertions.assertEquals(przedLiczba + 1, liczbaWpisowTypu("Zmiana stanu sprawy"));
    }

    @Test
    void aktualizacjaRol_zmieniaRoleUzytkownika() throws Exception {
        String uzytkownikId = utworzUzytkownika();
        long przedLiczba = liczbaWpisowTypu("Zmiana danych użytkownika");

        String akcjaId = uruchomAkcjeJakoAdministrator(Map.of(
                "typAkcji", "AKTUALIZACJA_ROL",
                "typCelu", "UZYTKOWNIK",
                "idCelow", List.of(uzytkownikId),
                "parametry", Map.of("dodajRole", List.of("kierownik"), "usunRole", List.of())
        ));

        czekajNaZakonczenie(akcjaId)
                .andExpect(jsonPath("$.przetworzone").value(1))
                .andExpect(jsonPath("$.bledne").value(0));

        mockMvc.perform(get("/api/uzytkownicy").param("wydzial", "Wydział Testowy Akcji Zbiorowych").with(jwtWithRole("administrator")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + uzytkownikId + "')].role").value(org.hamcrest.Matchers.hasItem(
                        org.hamcrest.Matchers.hasItem("kierownik"))));

        // Dokładnie jeden nowy wpis dziennika zdarzeń typu ZMIANA_DANYCH_UZYTKOWNIKA z przetwarzania akcji zbiorowej.
        org.junit.jupiter.api.Assertions.assertEquals(przedLiczba + 1, liczbaWpisowTypu("Zmiana danych użytkownika"));
    }

    /** AKTUALIZACJA_ROL zmienia role core (UzytkownikEntity.role), ta sama funkcja co
     * PUT /api/uzytkownicy/{id}/role (administrator-only w SecurityConfig) - requestMatchers po
     * ścieżce+metodzie nie widzi typAkcji w body generycznego POST /api/akcje-zbiorowe, więc
     * AkcjeZbioroweService.uruchom musi odrzucić to explicite dla ról innych niż administrator. */
    @Test
    void aktualizacjaRol_zabronionaDlaReferenta_403() throws Exception {
        String uzytkownikId = utworzUzytkownika();

        mockMvc.perform(post("/api/akcje-zbiorowe")
                        .with(jwtWithRole("referent")).with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "typAkcji", "AKTUALIZACJA_ROL",
                                "typCelu", "UZYTKOWNIK",
                                "idCelow", List.of(uzytkownikId),
                                "parametry", Map.of("dodajRole", List.of("administrator"), "usunRole", List.of())
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    void aktualizacjaDanych_konczySieZBledemKazdegoElementu_brakZdefiniowanegoKontraktu() throws Exception {
        String sprawaId = zarejestrujSprawe();

        String akcjaId = uruchomAkcje(Map.of(
                "typAkcji", "AKTUALIZACJA_DANYCH",
                "typCelu", "SPRAWA",
                "idCelow", List.of(sprawaId),
                "parametry", Map.of()
        ));

        czekajNaZakonczenie(akcjaId)
                .andExpect(jsonPath("$.lacznie").value(1))
                .andExpect(jsonPath("$.bledne").value(1))
                .andExpect(jsonPath("$.przetworzone").value(0))
                .andExpect(jsonPath("$.wynikiElementow[0].status").value("blad"))
                .andExpect(jsonPath("$.wynikiElementow[0].komunikat").value(
                        org.hamcrest.Matchers.containsString("AKTUALIZACJA_DANYCH")));
    }

    @Test
    void zmianaStanu_izolacjaPerElement_niepoprawneIdNiePsujeWalidnego() throws Exception {
        String sprawaId = zarejestrujSprawe();

        String akcjaId = uruchomAkcje(Map.of(
                "typAkcji", "ZMIANA_STANU",
                "typCelu", "SPRAWA",
                "idCelow", List.of(sprawaId, "brak-takiej-sprawy-" + UUID.randomUUID()),
                "parametry", Map.of("nowyStan", "W toku")
        ));

        czekajNaZakonczenie(akcjaId)
                .andExpect(jsonPath("$.lacznie").value(2))
                .andExpect(jsonPath("$.przetworzone").value(1))
                .andExpect(jsonPath("$.bledne").value(1))
                .andExpect(jsonPath("$.wynikiElementow[?(@.idCelu=='" + sprawaId + "')].status").value(
                        org.hamcrest.Matchers.hasItem("sukces")));
    }

    @Test
    void status_404GdyAkcjaNieIstnieje() throws Exception {
        mockMvc.perform(get("/api/akcje-zbiorowe/{id}/status", "brak-takiej-akcji").with(jwtWithRole("referent")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.komunikat").exists());
    }

    // --- helpers ---

    private String uruchomAkcje(Map<String, Object> body) throws Exception {
        return uruchomAkcjeJakoRola(body, "referent");
    }

    private String uruchomAkcjeJakoAdministrator(Map<String, Object> body) throws Exception {
        return uruchomAkcjeJakoRola(body, "administrator");
    }

    private String uruchomAkcjeJakoRola(Map<String, Object> body, String rola) throws Exception {
        String response = mockMvc.perform(post("/api/akcje-zbiorowe")
                        .with(jwtWithRole(rola)).with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.akcjaId").exists())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("akcjaId").asString();
    }

    private org.springframework.test.web.servlet.ResultActions czekajNaZakonczenie(String akcjaId) throws Exception {
        Awaitility.await().atMost(AWAIT_TIMEOUT).pollInterval(Duration.ofMillis(200)).untilAsserted(() -> {
            try {
                mockMvc.perform(get("/api/akcje-zbiorowe/{id}/status", akcjaId).with(jwtWithRole("referent")))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("zakonczona"));
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        });
        return mockMvc.perform(get("/api/akcje-zbiorowe/{id}/status", akcjaId).with(jwtWithRole("referent")))
                .andExpect(status().isOk());
    }

    /** Sprawa świeża (nie z seeda demo), stan początkowy "Zarejestrowana" (obieg-decyzja), legalnie
     * przechodząca do "W toku" - unika mutowania spr-1..spr-p3 używanych przez inne klasy testowe. */
    private String zarejestrujSprawe() throws Exception {
        Map<String, Object> request = Map.of(
                "definicjaObieguId", "obieg-decyzja",
                "jrwa", "6220",
                "kategoriaArchiwalna", "A",
                "tytul", "Sprawa testowa akcji zbiorowych " + UUID.randomUUID(),
                "obywatelId", "ob-1",
                "wydzial", "Wydział Ochrony Środowiska",
                "referent", "Test Referent"
        );
        String response = mockMvc.perform(post("/api/sprawy")
                        .with(jwtWithRole("referent")).with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asString();
    }

    private String utworzUzytkownika() throws Exception {
        Map<String, Object> request = Map.of(
                "login", "test.akcje." + UUID.randomUUID(),
                "imie", "Test",
                "nazwisko", "Akcje",
                "email", "test.akcje." + UUID.randomUUID() + "@urzad.gov.pl",
                "role", List.of("referent"),
                "wydzial", "Wydział Testowy Akcji Zbiorowych",
                "stanowisko", "Referent"
        );
        String response = mockMvc.perform(post("/api/uzytkownicy")
                        .with(jwtWithRole("administrator")).with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asString();
    }

    /** Liczba wpisów dziennika zdarzeń danego typu (wartość DB, np. "Zmiana stanu sprawy") - do
     * asercji na przyrost przy znanej liczbie wpisów seedowych tego typu. */
    private long liczbaWpisowTypu(String typOperacjiDbValue) {
        return wpisDziennikaZdarzenRepository.findAll().stream()
                .filter(w -> typOperacjiDbValue.equals(w.getTypOperacji()))
                .count();
    }
}
