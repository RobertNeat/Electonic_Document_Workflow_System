package robert_neat.document_api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.document_api.TestcontainersConfiguration;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static robert_neat.document_api.TestJwts.jwtWithNoRoles;
import static robert_neat.document_api.TestJwts.jwtWithRole;

/**
 * Testy autoryzacji na podstawie ról realmu Keycloak (`realm_access.roles`), przekrojowo przez
 * moduły - czy właściwa rola może wykonać akcję mutującą, czy nieuprawniona-ale-uwierzytelniona
 * rola dostaje 403, i czy token bez żadnej roli nadal ma dostęp do odczytów (otwartych dla każdego
 * uwierzytelnionego użytkownika), ale nie do akcji mutujących.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AuthorizationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    // --- rola uprawniona wykonuje akcję mutującą ---

    @Test
    @Transactional
    void referent_mozeZarejestrowacSprawe() throws Exception {
        mockMvc.perform(post("/api/sprawy")
                        .with(jwtWithRole("referent")).with(csrf())
                        .contentType("application/json")
                        .content(rejestracjaSprawyBody()))
                .andExpect(status().isCreated());
    }

    @Test
    @Transactional
    void administrator_mozeUtworzycUzytkownika() throws Exception {
        mockMvc.perform(post("/api/uzytkownicy")
                        .with(jwtWithRole("administrator")).with(csrf())
                        .contentType("application/json")
                        .content(utworzUzytkownikaBody("autz-admin")))
                .andExpect(status().isCreated());
    }

    @Test
    @Transactional
    void ksiegowosc_mozeZaimportowacWyciagCamt053() throws Exception {
        byte[] xml = getClass().getResourceAsStream("/camt053/wyciag-dopasowanie.xml").readAllBytes();
        org.springframework.mock.web.MockMultipartFile plik =
                new org.springframework.mock.web.MockMultipartFile("plik", "wyciag.xml", "application/xml", xml);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/api/import-camt053").file(plik)
                        .with(jwtWithRole("ksiegowosc")).with(csrf()))
                .andExpect(status().isOk());
    }

    // --- rola uwierzytelniona, ale nieuprawniona do danej akcji - 403 ---

    @Test
    @Transactional
    void referent_niemozeUtworzycUzytkownika_403() throws Exception {
        mockMvc.perform(post("/api/uzytkownicy")
                        .with(jwtWithRole("referent")).with(csrf())
                        .contentType("application/json")
                        .content(utworzUzytkownikaBody("autz-referent")))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void ksiegowosc_niemozeZarejestrowacSprawy_403() throws Exception {
        mockMvc.perform(post("/api/sprawy")
                        .with(jwtWithRole("ksiegowosc")).with(csrf())
                        .contentType("application/json")
                        .content(rejestracjaSprawyBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void referent_niemozeEdytowacDefinicjiObiegu_403() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/definicje-obiegow/{id}", "obieg-decyzja")
                        .with(jwtWithRole("referent")).with(csrf())
                        .contentType("application/json")
                        .content("{\"nazwa\":\"X\",\"opis\":null}"))
                .andExpect(status().isForbidden());
    }

    // --- token bez żadnej roli: odczyt OK, mutacja 403 ---

    @Test
    void brakRoli_odczytWciazDostepny200() throws Exception {
        mockMvc.perform(get("/api/sprawy/spr-1").with(jwtWithNoRoles()))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void brakRoli_mutacjaZablokowana403() throws Exception {
        mockMvc.perform(post("/api/sprawy")
                        .with(jwtWithNoRoles()).with(csrf())
                        .contentType("application/json")
                        .content(rejestracjaSprawyBody()))
                .andExpect(status().isForbidden());
    }

    // --- weryfikacja faktycznego łańcucha filtrów: nagłówek Bearer + JwtAuthenticationConverter ---

    @Test
    @Transactional
    void realnyBearerToken_adminMozeUtworzycUzytkownika_brakRoliDostaje403() throws Exception {
        org.mockito.Mockito.when(jwtDecoder.decode("token-admin")).thenReturn(jwtZRolami("administrator"));
        org.mockito.Mockito.when(jwtDecoder.decode("token-brak-roli")).thenReturn(jwtZRolami());

        mockMvc.perform(post("/api/uzytkownicy")
                        .header("Authorization", "Bearer token-admin")
                        .with(csrf())
                        .contentType("application/json")
                        .content(utworzUzytkownikaBody("autz-bearer-admin")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/uzytkownicy")
                        .header("Authorization", "Bearer token-brak-roli")
                        .with(csrf())
                        .contentType("application/json")
                        .content(utworzUzytkownikaBody("autz-bearer-norole")))
                .andExpect(status().isForbidden());
    }

    // --- helpers ---

    private Jwt jwtZRolami(String... roles) {
        return Jwt.withTokenValue("token-value")
                .header("alg", "none")
                .claim("sub", "test-subject")
                .claim("realm_access", Map.of("roles", List.of(roles)))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
    }

    private String rejestracjaSprawyBody() {
        return """
                {
                  "definicjaObieguId": "obieg-decyzja",
                  "jrwa": "6220",
                  "kategoriaArchiwalna": "A",
                  "tytul": "Sprawa testowa autoryzacji",
                  "obywatelId": "ob-1",
                  "wydzial": "Wydział Ochrony Środowiska",
                  "referent": "Testowy Referent"
                }
                """;
    }

    private String utworzUzytkownikaBody(String loginSuffix) {
        return """
                {
                  "login": "%s",
                  "imie": "Test",
                  "nazwisko": "Autoryzacji",
                  "email": "%s@urzad.gov.pl",
                  "role": ["referent"],
                  "wydzial": "Wydział Ochrony Środowiska",
                  "stanowisko": "Referent"
                }
                """.formatted(loginSuffix, loginSuffix);
    }
}
