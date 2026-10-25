package robert_neat.document_api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.document_api.TestcontainersConfiguration;
import robert_neat.document_api.entity.DefinicjaObieguEntity;
import robert_neat.document_api.entity.DokumentEntity;
import robert_neat.document_api.entity.PrzejscieObieguEntity;
import robert_neat.document_api.entity.SprawaEntity;
import robert_neat.document_api.entity.TypDokumentuEntity;
import robert_neat.document_api.repository.DefinicjaObieguRepository;
import robert_neat.document_api.repository.DokumentRepository;
import robert_neat.document_api.repository.PrzejscieObieguRepository;
import robert_neat.document_api.repository.SprawaRepository;
import robert_neat.document_api.repository.TypDokumentuRepository;
import robert_neat.document_api.repository.WpisDziennikaZdarzenRepository;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static robert_neat.document_api.TestJwts.jwtWithRole;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy integracyjne modułów Sprawy i Obywatele na realnej bazie Postgres (Testcontainers),
 * z liquibase contextem "demo" (domyślny) dającym seedowane dane (ob-1..ob-3, spr-1..spr-3, spr-p1..p3).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class SprawyObywateleControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WpisDziennikaZdarzenRepository wpisDziennikaZdarzenRepository;

    @Autowired
    private SprawaRepository sprawaRepository;

    @Autowired
    private DokumentRepository dokumentRepository;

    @Autowired
    private TypDokumentuRepository typDokumentuRepository;

    @Autowired
    private PrzejscieObieguRepository przejscieObieguRepository;

    @Autowired
    private DefinicjaObieguRepository definicjaObieguRepository;

    // --- obywatele ---

    @Test
    void getObywatelById_zwracaDaneZSeeda() throws Exception {
        mockMvc.perform(get("/api/obywatele/ob-1").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imie").value("Jan"))
                .andExpect(jsonPath("$.nazwisko").value("Kowalski"))
                .andExpect(jsonPath("$.adres.miejscowosc").value("Warszawa"));
    }

    @Test
    void getObywatelById_404GdyNieIstnieje() throws Exception {
        mockMvc.perform(get("/api/obywatele/brak-takiego").with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.komunikat").exists());
    }

    @Test
    void searchObywatele_czesciowePasekCaseInsensitive() throws Exception {
        mockMvc.perform(get("/api/obywatele").param("query", "kowal").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nazwisko").value("Kowalski"));
    }

    @Test
    void searchObywatele_poNip() throws Exception {
        mockMvc.perform(get("/api/obywatele").param("query", "5261234567").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("ob-3"));
    }

    @Test
    void getWithoutToken_401() throws Exception {
        mockMvc.perform(get("/api/obywatele/ob-1"))
                .andExpect(status().isUnauthorized());
    }

    // --- sprawy: listowanie/filtrowanie ---

    @Test
    void listaSpraw_filtrPoStanieIWydziale() throws Exception {
        mockMvc.perform(get("/api/sprawy")
                        .param("wydzial", "Wydział Ochrony Środowiska")
                        .param("stan", "W toku")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dane").isArray());
    }

    @Test
    void listaSpraw_filtrPoObywatelId() throws Exception {
        mockMvc.perform(get("/api/sprawy").param("obywatelId", "ob-1").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liczbaWszystkich").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    void listaSpraw_sortTerminUstawowyAsc() throws Exception {
        String response = mockMvc.perform(get("/api/sprawy")
                        .param("sort", "terminUstawowy,asc")
                        .param("rozmiarStrony", "100")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        tools.jackson.databind.JsonNode dane = objectMapper.readTree(response).get("dane");
        for (int i = 1; i < dane.size(); i++) {
            tools.jackson.databind.JsonNode poprzedniNode = dane.get(i - 1).get("terminUstawowy");
            tools.jackson.databind.JsonNode aktualnyNode = dane.get(i).get("terminUstawowy");
            if (poprzedniNode.isNull() || aktualnyNode.isNull()) {
                continue;
            }
            String poprzedni = poprzedniNode.asText();
            String aktualny = aktualnyNode.asText();
            org.junit.jupiter.api.Assertions.assertTrue(poprzedni.compareTo(aktualny) <= 0,
                    "Termin ustawowy powinien być w porządku rosnącym: " + poprzedni + " vs " + aktualny);
        }
    }

    @Test
    void listaSpraw_sortZnakSprawyDesc() throws Exception {
        String response = mockMvc.perform(get("/api/sprawy")
                        .param("sort", "znakSprawy,desc")
                        .param("rozmiarStrony", "100")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        tools.jackson.databind.JsonNode dane = objectMapper.readTree(response).get("dane");
        for (int i = 1; i < dane.size(); i++) {
            String poprzedni = dane.get(i - 1).get("znakSprawy").asText();
            String aktualny = dane.get(i).get("znakSprawy").asText();
            org.junit.jupiter.api.Assertions.assertTrue(poprzedni.compareTo(aktualny) >= 0,
                    "Znak sprawy powinien być w porządku malejącym: " + poprzedni + " vs " + aktualny);
        }
    }

    @Test
    void listaSpraw_sortPoleNieDozwolone400() throws Exception {
        mockMvc.perform(get("/api/sprawy")
                        .param("sort", "haslo,asc")
                        .with(jwt()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listaSpraw_sortNieprawidlowyKierunek400() throws Exception {
        mockMvc.perform(get("/api/sprawy")
                        .param("sort", "znakSprawy,invalid")
                        .with(jwt()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listaSpraw_bezSortuDzialaJakDotychczas() throws Exception {
        mockMvc.perform(get("/api/sprawy").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dane").isArray());
    }

    @Test
    void getSprawaById_zwracaDaneZSeeda() throws Exception {
        mockMvc.perform(get("/api/sprawy/spr-1").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.znakSprawy").value("WO.6220.14.2026"));
    }

    @Test
    void getSprawaById_404GdyNieIstnieje() throws Exception {
        mockMvc.perform(get("/api/sprawy/brak-takiej").with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMetryka_404GdySprawaNieIstnieje() throws Exception {
        mockMvc.perform(get("/api/sprawy/brak-takiej/metryka").with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMetryka_listaDlaSpr1() throws Exception {
        mockMvc.perform(get("/api/sprawy/spr-1/metryka").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    void definicjeObiegow_zawierajaPrzejscia() throws Exception {
        mockMvc.perform(get("/api/definicje-obiegow").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='obieg-decyzja')].przejscia.length()").value(org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.greaterThan(0))));
    }

    @Test
    @Transactional
    void edytujDefinicjeObiegu_sukcesAktualizujeNazweIOpis() throws Exception {
        mockMvc.perform(put("/api/definicje-obiegow/{id}", "obieg-decyzja")
                        .with(jwtWithRole("administrator")).with(csrf())
                        .contentType("application/json")
                        .content("{\"nazwa\":\"Obieg decyzji - zmieniona nazwa\",\"opis\":\"Nowy opis\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nazwa").value("Obieg decyzji - zmieniona nazwa"))
                .andExpect(jsonPath("$.opis").value("Nowy opis"));
    }

    @Test
    @Transactional
    void edytujDefinicjeObiegu_404GdyNieIstnieje() throws Exception {
        mockMvc.perform(put("/api/definicje-obiegow/{id}", "brak-takiej")
                        .with(jwtWithRole("administrator")).with(csrf())
                        .contentType("application/json")
                        .content("{\"nazwa\":\"X\",\"opis\":null}"))
                .andExpect(status().isNotFound());
    }

    // --- sprawy: rejestracja ---

    @Test
    @Transactional
    void rejestrujSprawe_generujeZnakIMetrykeRejestracji() throws Exception {
        long przedLiczba = liczbaWpisowTypu("Rejestracja sprawy");
        String rok = String.valueOf(Year.now().getValue());
        String body = """
                {
                  "definicjaObieguId": "obieg-decyzja",
                  "jrwa": "6220",
                  "kategoriaArchiwalna": "A",
                  "tytul": "Testowy wniosek",
                  "obywatelId": "ob-1",
                  "wydzial": "Wydział Ochrony Środowiska",
                  "referent": "Testowy Referent"
                }
                """;
        mockMvc.perform(post("/api/sprawy")
                        .with(jwtWithRole("referent")).with(csrf())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.znakSprawy").value(org.hamcrest.Matchers.matchesPattern("WO\\.6220\\.\\d+\\." + rok)))
                .andExpect(jsonPath("$.stan").value("Zarejestrowana"))
                .andExpect(jsonPath("$.zamknieta").value(false));

        // Dokładnie jeden nowy wpis dziennika zdarzeń typu REJESTRACJA_SPRAWY - seed już zawiera
        // wpisy tego typu (ev-1, ev-8), stąd asercja na przyrost w ramach TEGO typu, nie na
        // wartość absolutną ani na łączną liczbę wpisów wszystkich typów.
        org.junit.jupiter.api.Assertions.assertEquals(przedLiczba + 1, liczbaWpisowTypu("Rejestracja sprawy"));
    }

    @Test
    @Transactional
    void rejestrujSprawe_walidacjaBrakujacychPolZwraca400() throws Exception {
        mockMvc.perform(post("/api/sprawy")
                        .with(jwtWithRole("referent")).with(csrf())
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void rejestrujSprawe_nieznanyWydzial400() throws Exception {
        String body = """
                {
                  "definicjaObieguId": "obieg-decyzja",
                  "jrwa": "6220",
                  "kategoriaArchiwalna": "A",
                  "tytul": "Testowy wniosek",
                  "obywatelId": "ob-1",
                  "wydzial": "Wydział Nieistniejący",
                  "referent": "Testowy Referent"
                }
                """;
        mockMvc.perform(post("/api/sprawy")
                        .with(jwtWithRole("referent")).with(csrf())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // --- sprawy: zmiana stanu ---

    @Test
    @Transactional
    void zmienStan_legalnePrzejscie200() throws Exception {
        long przedRejestracja = liczbaWpisowTypu("Rejestracja sprawy");
        long przedZmianaStanu = liczbaWpisowTypu("Zmiana stanu sprawy");
        String bodyRejestracja = """
                {
                  "definicjaObieguId": "obieg-decyzja",
                  "jrwa": "6220",
                  "kategoriaArchiwalna": "A",
                  "tytul": "Sprawa do przejscia",
                  "obywatelId": "ob-1",
                  "wydzial": "Wydział Ochrony Środowiska",
                  "referent": "Testowy Referent"
                }
                """;
        String response = mockMvc.perform(post("/api/sprawy")
                        .with(jwtWithRole("referent")).with(csrf())
                        .contentType("application/json")
                        .content(bodyRejestracja))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(patch("/api/sprawy/{id}/stan", id)
                        .with(jwtWithRole("referent")).with(csrf())
                        .contentType("application/json")
                        .content("{\"nowyStan\":\"W toku\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stan").value("W toku"))
                .andExpect(jsonPath("$.zamknieta").value(false));

        // Rejestracja sprawy (REJESTRACJA_SPRAWY) + zmiana stanu (ZMIANA_STANU_SPRAWY) - jeden nowy
        // wpis każdego typu w tym teście.
        org.junit.jupiter.api.Assertions.assertEquals(przedRejestracja + 1, liczbaWpisowTypu("Rejestracja sprawy"));
        org.junit.jupiter.api.Assertions.assertEquals(przedZmianaStanu + 1, liczbaWpisowTypu("Zmiana stanu sprawy"));
    }

    @Test
    @Transactional
    void zmienStan_nielegalnePrzejscie409() throws Exception {
        // spr-1 jest w stanie "W toku" wg seeda; "Zamknięta" nie jest dozwolonym przejściem z "W toku".
        mockMvc.perform(patch("/api/sprawy/spr-1/stan")
                        .with(jwtWithRole("referent")).with(csrf())
                        .contentType("application/json")
                        .content("{\"nowyStan\":\"Zamknięta\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.komunikat").exists());
    }

    @Test
    @Transactional
    void zmienStan_doStanuTerminalnegoZamykaSprawe() throws Exception {
        // spr-2 (obieg-skarga) jest w stanie "Odpowiedź przygotowana"; "Zakończona" jest ostatnim
        // stanem w definicji -> powinno ustawić zamknieta=true.
        mockMvc.perform(patch("/api/sprawy/spr-2/stan")
                        .with(jwtWithRole("referent")).with(csrf())
                        .contentType("application/json")
                        .content("{\"nowyStan\":\"Zakończona\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stan").value("Zakończona"))
                .andExpect(jsonPath("$.zamknieta").value(true));
    }

    @Test
    @Transactional
    void zmienStan_404GdySprawaNieIstnieje() throws Exception {
        mockMvc.perform(patch("/api/sprawy/brak-takiej/stan")
                        .with(jwtWithRole("referent")).with(csrf())
                        .contentType("application/json")
                        .content("{\"nowyStan\":\"W toku\"}"))
                .andExpect(status().isNotFound());
    }

    // --- sprawy: zmiana stanu - walidacja roli/dokumentów przejścia (prz-decyzja-3 w seedzie:
    // obieg-decyzja "Do akceptacji" -> "Zakończona", wymagana_rola=kierownik, wymagany typ td-3) ---

    @Test
    @Transactional
    void zmienStan_brakWymaganejRoli409INieZmieniaStanu() throws Exception {
        // spr-1 ma już w seedzie dokument typu td-3 (d-2) - do testu samej roli, bez ingerencji
        // wymogu dokumentu, wystarczy istniejąca sprawa seedowa przesunięta do "Do akceptacji".
        przesunSpraweDoStanu("spr-1", "Do akceptacji");

        mockMvc.perform(patch("/api/sprawy/spr-1/stan")
                        .with(jwtWithRole("referent")).with(csrf())
                        .contentType("application/json")
                        .content("{\"nowyStan\":\"Zakończona\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.komunikat").value(org.hamcrest.Matchers.containsString("kierownik")));

        org.junit.jupiter.api.Assertions.assertEquals("Do akceptacji", sprawaRepository.findById("spr-1").orElseThrow().getStan());
    }

    @Test
    @Transactional
    void zmienStan_brakWymaganegoDokumentu409INieZmieniaStanu() throws Exception {
        // Nowa sprawa (obieg-decyzja) bez żadnych dokumentów - w odróżnieniu od spr-1 z seeda,
        // który ma już dokument td-3, więc nie nadaje się do sprawdzenia braku dokumentu.
        String id = zarejestrujNowaSpraweDecyzja();
        przesunSpraweDoStanu(id, "Do akceptacji");

        mockMvc.perform(patch("/api/sprawy/{id}/stan", id)
                        .with(jwtWithRole("kierownik")).with(csrf())
                        .contentType("application/json")
                        .content("{\"nowyStan\":\"Zakończona\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.komunikat").value(org.hamcrest.Matchers.containsString("td-3")));

        org.junit.jupiter.api.Assertions.assertEquals("Do akceptacji", sprawaRepository.findById(id).orElseThrow().getStan());
    }

    @Test
    @Transactional
    void zmienStan_rolaIDokumentSpelnione200() throws Exception {
        String id = zarejestrujNowaSpraweDecyzja();
        przesunSpraweDoStanu(id, "Do akceptacji");
        dodajDokument(id, "td-3");

        mockMvc.perform(patch("/api/sprawy/{id}/stan", id)
                        .with(jwtWithRole("kierownik")).with(csrf())
                        .contentType("application/json")
                        .content("{\"nowyStan\":\"Zakończona\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stan").value("Zakończona"));
    }

    @Test
    @Transactional
    void zmienStan_rolaWSecieWieluRolIDokumentSpelnione200() throws Exception {
        String id = zarejestrujNowaSpraweDecyzja();
        przesunSpraweDoStanu(id, "Do akceptacji");
        dodajDokument(id, "td-3");

        mockMvc.perform(patch("/api/sprawy/{id}/stan", id)
                        .with(jwtWithRole("referent", "kierownik")).with(csrf())
                        .contentType("application/json")
                        .content("{\"nowyStan\":\"Zakończona\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stan").value("Zakończona"));
    }

    @Test
    @Transactional
    void zmienStan_rozgalezienieKtoregokolwiekKandydataDopuszcza200() throws Exception {
        // Druga odnoga rozgałęzienia z tego samego zStanu ("Do akceptacji" -> "Zakończona"),
        // dostępna dla roli "administrator", bez wymogu dokumentu - analogicznie do seedowego
        // prz-decyzja-3 (kierownik + td-3). Zgodnie z semantyką frontendu (przejsciaZeStanu w
        // sprawy.utils.ts) wygrywa odnoga dostępna dla wołającego, niezależnie od kolejności
        // wierszy w bazie - test dowodzi, że walidacja serwera nie ogranicza się do PIERWSZEGO
        // znalezionego wiersza.
        PrzejscieObieguEntity drugaOdnoga = new PrzejscieObieguEntity();
        drugaOdnoga.setId("test-prz-decyzja-3b");
        DefinicjaObieguEntity obiegDecyzja = definicjaObieguRepository.findById("obieg-decyzja").orElseThrow();
        drugaOdnoga.setDefinicjaObiegu(obiegDecyzja);
        drugaOdnoga.setZstanu("Do akceptacji");
        drugaOdnoga.setWymaganaRola("administrator");
        drugaOdnoga.setDoStanow(List.of("Zakończona"));
        przejscieObieguRepository.save(drugaOdnoga);

        String id = zarejestrujNowaSpraweDecyzja();
        przesunSpraweDoStanu(id, "Do akceptacji");

        mockMvc.perform(patch("/api/sprawy/{id}/stan", id)
                        .with(jwtWithRole("administrator")).with(csrf())
                        .contentType("application/json")
                        .content("{\"nowyStan\":\"Zakończona\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stan").value("Zakończona"));

        // Referent nie spełnia ŻADNEJ odnogi (ani kierownik+td-3, ani administrator) -> 409.
        String id2 = zarejestrujNowaSpraweDecyzja();
        przesunSpraweDoStanu(id2, "Do akceptacji");
        mockMvc.perform(patch("/api/sprawy/{id}/stan", id2)
                        .with(jwtWithRole("referent")).with(csrf())
                        .contentType("application/json")
                        .content("{\"nowyStan\":\"Zakończona\"}"))
                .andExpect(status().isConflict());

        // Druga odnoga (kierownik+td-3) wciąż dostępna niezależnie od kolejności wierszy w bazie -
        // dowodzi, że walidacja nie zatrzymuje się na pierwszym napotkanym kandydacie.
        String id3 = zarejestrujNowaSpraweDecyzja();
        przesunSpraweDoStanu(id3, "Do akceptacji");
        dodajDokument(id3, "td-3");
        mockMvc.perform(patch("/api/sprawy/{id}/stan", id3)
                        .with(jwtWithRole("kierownik")).with(csrf())
                        .contentType("application/json")
                        .content("{\"nowyStan\":\"Zakończona\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stan").value("Zakończona"));
    }

    /** Rejestruje nową sprawę w obiegu obieg-decyzja (bez żadnych dokumentów) - do testów walidacji
     * dokumentu na przejściu "Do akceptacji" -> "Zakończona", gdzie seedowa spr-1 nie nadaje się
     * (ma już dokument wymaganego typu td-3). */
    private String zarejestrujNowaSpraweDecyzja() throws Exception {
        String body = """
                {
                  "definicjaObieguId": "obieg-decyzja",
                  "jrwa": "6220",
                  "kategoriaArchiwalna": "A",
                  "tytul": "Sprawa testowa walidacji przejscia",
                  "obywatelId": "ob-1",
                  "wydzial": "Wydział Ochrony Środowiska",
                  "referent": "Testowy Referent"
                }
                """;
        String response = mockMvc.perform(post("/api/sprawy")
                        .with(jwtWithRole("referent")).with(csrf())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    /** Przesuwa sprawę do żądanego stanu bezpośrednio przez repozytorium - test izoluje walidację
     * roli/dokumentu na przejściu docelowym, bez zależności od przejść pośrednich (które same
     * niczego nie wymagają w seedzie). */
    private void przesunSpraweDoStanu(String sprawaId, String stan) {
        SprawaEntity sprawa = sprawaRepository.findById(sprawaId).orElseThrow();
        sprawa.setStan(stan);
        sprawaRepository.save(sprawa);
    }

    private void dodajDokument(String sprawaId, String typDokumentuId) {
        TypDokumentuEntity typ = typDokumentuRepository.findById(typDokumentuId).orElseThrow();
        SprawaEntity sprawa = sprawaRepository.findById(sprawaId).orElseThrow();
        DokumentEntity dokument = new DokumentEntity();
        dokument.setId(UUID.randomUUID().toString());
        dokument.setSprawa(sprawa);
        dokument.setTypDokumentu(typ);
        dokument.setNazwa("Dokument testowy " + typDokumentuId);
        dokument.setAktualnaWersja(1);
        dokument.setDataUtworzenia(LocalDate.now());
        dokumentRepository.save(dokument);
    }

    /** Liczba wpisów dziennika zdarzeń danego typu (wartość DB, np. "Rejestracja sprawy") - do
     * asercji na przyrost przy znanej liczbie wpisów seedowych tego typu. */
    private long liczbaWpisowTypu(String typOperacjiDbValue) {
        return wpisDziennikaZdarzenRepository.findAll().stream()
                .filter(w -> typOperacjiDbValue.equals(w.getTypOperacji()))
                .count();
    }
}
