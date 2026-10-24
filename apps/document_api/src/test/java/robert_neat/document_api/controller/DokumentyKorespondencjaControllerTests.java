package robert_neat.document_api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.document_api.TestcontainersConfiguration;
import robert_neat.document_api.entity.WersjaDokumentuEntity;
import robert_neat.document_api.repository.WersjaDokumentuRepository;
import robert_neat.document_api.repository.WpisDziennikaZdarzenRepository;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static robert_neat.document_api.TestJwts.jwtWithRole;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy integracyjne modułów Dokumenty i Korespondencja na realnej bazie Postgres (Testcontainers),
 * z liquibase contextem "demo" (domyślny) dającym seedowane dane: dokumenty d-1/d-2, wersje
 * wd-1 (niepodpisana, wersja 1 d-2)/wd-2 (podpisana, wersja 2 = aktualna d-2), przesyłki p-1/p-2.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class DokumentyKorespondencjaControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WersjaDokumentuRepository wersjaDokumentuRepository;

    @Autowired
    private WpisDziennikaZdarzenRepository wpisDziennikaZdarzenRepository;

    // --- dokumenty: listowanie/szczegoly ---

    @Test
    void listaDokumentow_bezFiltrowZwracaWszystkieZeZnakiemIStatusemPodpisu() throws Exception {
        mockMvc.perform(get("/api/dokumenty").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$[?(@.id=='d-2')].znakSprawy").value(hasItem("WO.6220.14.2026")))
                .andExpect(jsonPath("$[?(@.id=='d-2')].statusPodpisuAktualnej").value(hasItem("poprawny")))
                .andExpect(jsonPath("$[?(@.id=='d-1')].statusPodpisuAktualnej").value(hasItem("brak")));
    }

    @Test
    void listaDokumentow_filtrPoSprawieITypie() throws Exception {
        mockMvc.perform(get("/api/dokumenty")
                        .param("sprawaId", "spr-1")
                        .param("typDokumentuId", "td-3")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("d-2"));
    }

    @Test
    void getDokumentById_zwracaDaneZSeeda() throws Exception {
        mockMvc.perform(get("/api/dokumenty/d-2").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nazwa").value("Decyzja środowiskowa"))
                .andExpect(jsonPath("$.aktualnaWersja").value(2))
                .andExpect(jsonPath("$.sprawaId").value("spr-1"));
    }

    @Test
    void getDokumentById_404GdyNieIstnieje() throws Exception {
        mockMvc.perform(get("/api/dokumenty/brak-takiego").with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.komunikat").exists());
    }

    // --- dokumenty: wersje ---

    @Test
    void listaWersji_malejacoPoNumerzeWersji() throws Exception {
        mockMvc.perform(get("/api/dokumenty/d-2/wersje").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].wersja").value(2))
                .andExpect(jsonPath("$[0].podpisana").value(true))
                .andExpect(jsonPath("$[0].statusPodpisu").value("poprawny"))
                .andExpect(jsonPath("$[1].wersja").value(1))
                .andExpect(jsonPath("$[1].statusPodpisu").value("brak"));
    }

    @Test
    void listaWersji_404GdyDokumentNieIstnieje() throws Exception {
        mockMvc.perform(get("/api/dokumenty/brak-takiego/wersje").with(jwt()))
                .andExpect(status().isNotFound());
    }

    // --- szablony ---

    @Test
    void listaSzablonow_filtrPoTypieDokumentu() throws Exception {
        mockMvc.perform(get("/api/szablony").param("typDokumentuId", "td-3").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("sz-1"));
    }

    @Test
    void historiaSzablonu_zwracaWersjeTegoSzablonu() throws Exception {
        mockMvc.perform(get("/api/szablony/sz-1/historia").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("sz-1"))
                .andExpect(jsonPath("$[0].wersja").value(3));
    }

    @Test
    void historiaSzablonu_404GdyNieIstnieje() throws Exception {
        mockMvc.perform(get("/api/szablony/brak-takiego/historia").with(jwt()))
                .andExpect(status().isNotFound());
    }

    // --- przesylki: listowanie/szczegoly ---

    @Test
    void listaPrzesylek_filtrPoStatusieKanaleKierunku() throws Exception {
        mockMvc.perform(get("/api/przesylki")
                        .param("status", "doreczone")
                        .param("kanal", "e-doreczenia")
                        .param("kierunek", "wychodzaca")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("p-1"))
                .andExpect(jsonPath("$[0].historiaStatusow.length()").value(2))
                .andExpect(jsonPath("$[0].historiaStatusow[0].status").value("nadane"))
                .andExpect(jsonPath("$[0].historiaStatusow[1].status").value("doreczone"));
    }

    @Test
    void listaPrzesylek_filtrPoObywatelIdZwracaTylkoJegoPrzesylki() throws Exception {
        // p-1 nalezy do ob-1, p-2 do ob-2 (100-seed.xml) - sprawdzamy tylko przynaleznosc id, bez
        // zalozen o statusie/kanale (PrzesylkaStatusuConsumerTests moze w miedzyczasie nadawac wlasne
        // przesylki kanalem "email", ale nie modyfikuje p-1/p-2).
        mockMvc.perform(get("/api/przesylki")
                        .param("obywatelId", "ob-1")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].obywatelId").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("ob-1"))))
                .andExpect(jsonPath("$[*].id").value(hasItem("p-1")))
                .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.not(hasItem("p-2"))));
    }

    @Test
    void listaPrzesylek_filtrPoObywatelIdOrazStatusieDzialaLacznieAND() throws Exception {
        mockMvc.perform(get("/api/przesylki")
                        .param("obywatelId", "ob-1")
                        .param("status", "doreczone")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(hasItem("p-1")));

        // ob-2 (wlasciciel p-2, status "nadane" w seedzie) nie moze wystapic dla status=doreczone.
        mockMvc.perform(get("/api/przesylki")
                        .param("obywatelId", "ob-2")
                        .param("status", "doreczone")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(org.hamcrest.Matchers.not(hasItem("p-2"))));
    }

    @Test
    void listaPrzesylek_filtrPoNieznanymObywatelIdZwracaPustaListe() throws Exception {
        mockMvc.perform(get("/api/przesylki")
                        .param("obywatelId", "brak-takiego-obywatela")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getPrzesylkaById_zawieraHistorieStatusow() throws Exception {
        mockMvc.perform(get("/api/przesylki/p-2").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numerReferencyjny").value("PL2026000998"))
                .andExpect(jsonPath("$.historiaStatusow.length()").value(1))
                .andExpect(jsonPath("$.historiaStatusow[0].status").value("nadane"));
    }

    @Test
    void getPrzesylkaById_404GdyNieIstnieje() throws Exception {
        mockMvc.perform(get("/api/przesylki/brak-takiej").with(jwt()))
                .andExpect(status().isNotFound());
    }

    // --- przesylki: nadanie pisma ---

    @Test
    @Transactional
    void nadajPismo_sukcesDlaPodpisanejAktualnejWersji() throws Exception {
        long przedLiczba = liczbaWpisowTypu("Wysyłka pisma");
        String body = """
                {
                  "sprawaId": "spr-1",
                  "dokumentId": "d-2",
                  "obywatelId": "ob-1",
                  "kanal": "e-doreczenia"
                }
                """;
        mockMvc.perform(post("/api/przesylki")
                        .with(jwtWithRole("kancelaria")).with(csrf())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("nadane"))
                .andExpect(jsonPath("$.kierunek").value("wychodzaca"))
                .andExpect(jsonPath("$.numerReferencyjny").value(org.hamcrest.Matchers.matchesPattern("ADE/\\d+/\\d+")))
                .andExpect(jsonPath("$.historiaStatusow.length()").value(1))
                .andExpect(jsonPath("$.historiaStatusow[0].status").value("nadane"));

        // Dokładnie jeden nowy wpis dziennika zdarzeń typu WYSYLKA_PISMA.
        org.junit.jupiter.api.Assertions.assertEquals(przedLiczba + 1, liczbaWpisowTypu("Wysyłka pisma"));
    }

    @Test
    @Transactional
    void nadajPismo_400GdyAktualnaWersjaNieIstnieje() throws Exception {
        // d-1 ma aktualna_wersja=1, ale brak jakiegokolwiek wiersza wersja_dokumentu dla d-1 w seedzie.
        String body = """
                {
                  "sprawaId": "spr-1",
                  "dokumentId": "d-1",
                  "obywatelId": "ob-1",
                  "kanal": "email"
                }
                """;
        mockMvc.perform(post("/api/przesylki")
                        .with(jwtWithRole("kancelaria")).with(csrf())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.komunikat").exists());
    }

    @Test
    @Transactional
    void nadajPismo_400GdyAktualnaWersjaNiepodpisana() throws Exception {
        // Przełączamy wd-2 (aktualna wersja d-2) na niepodpisaną - transakcja wycofa się po teście.
        WersjaDokumentuEntity wersja = wersjaDokumentuRepository.findById("wd-2").orElseThrow();
        wersja.setPodpisana(false);
        wersjaDokumentuRepository.save(wersja);
        wersjaDokumentuRepository.flush();

        String body = """
                {
                  "sprawaId": "spr-1",
                  "dokumentId": "d-2",
                  "obywatelId": "ob-1",
                  "kanal": "operator_pocztowy"
                }
                """;
        mockMvc.perform(post("/api/przesylki")
                        .with(jwtWithRole("kancelaria")).with(csrf())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void nadajPismo_404GdySprawaNieIstnieje() throws Exception {
        String body = """
                {
                  "sprawaId": "brak-takiej",
                  "dokumentId": "d-2",
                  "obywatelId": "ob-1",
                  "kanal": "email"
                }
                """;
        mockMvc.perform(post("/api/przesylki")
                        .with(jwtWithRole("kancelaria")).with(csrf())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void nadajPismo_walidacjaBrakujacychPolZwraca400() throws Exception {
        mockMvc.perform(post("/api/przesylki")
                        .with(jwtWithRole("kancelaria")).with(csrf())
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    /** Liczba wpisów dziennika zdarzeń danego typu (wartość DB, np. "Wysyłka pisma") - do asercji
     * na przyrost przy znanej liczbie wpisów seedowych tego typu. */
    private long liczbaWpisowTypu(String typOperacjiDbValue) {
        return wpisDziennikaZdarzenRepository.findAll().stream()
                .filter(w -> typOperacjiDbValue.equals(w.getTypOperacji()))
                .count();
    }
}
