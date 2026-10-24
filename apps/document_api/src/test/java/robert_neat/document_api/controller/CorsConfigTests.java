package robert_neat.document_api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.document_api.TestcontainersConfiguration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static robert_neat.document_api.TestJwts.jwtWithRole;

/**
 * Testy CORS (SecurityConfig.corsConfigurationSource) - preflight OPTIONS musi przejść przez
 * filter chain PRZED oauth2ResourceServer (inaczej 401 zanim nagłówki CORS zostałyby dodane,
 * zob. uzasadnienie w komentarzu SecurityConfig.filterChain).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class CorsConfigTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void preflightZDozwolonegoOriginu_dostajeNagłowekAccessControlAllowOrigin() throws Exception {
        mockMvc.perform(options("/api/sprawy")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }

    /** Spring CorsProcessor (DefaultCorsProcessor.rejectRequest) odpowiada 403 bez żadnych
     * nagłówków CORS dla originu spoza konfiguracji - request nie dochodzi nawet do
     * normalnych reguł autoryzacji/uwierzytelnienia. */
    @Test
    void preflightZNiedozwolonegoOriginu_nieDostajeNagłowkaCors() throws Exception {
        mockMvc.perform(options("/api/sprawy")
                        .header("Origin", "http://evil.example.com")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    /** Odpowiedź 403 z normalnej reguły autoryzacji (nie preflight) musi RÓWNIEŻ dostać nagłówek
     * CORS dla dozwolonego originu - inaczej przeglądarka pokazałaby status 0 (zablokowane przez
     * CORS), a `document_web` wyświetliłby komunikat o braku połączenia zamiast "brak uprawnień"
     * (zob. punkt 8 briefu cyklu). */
    @Test
    @Transactional
    void odpowiedzForbidden_dlaDozwolonegoOriginu_takzeDostajeNagłowekCors() throws Exception {
        mockMvc.perform(post("/api/sprawy")
                        .with(jwtWithRole("ksiegowosc")).with(csrf())
                        .header("Origin", "http://localhost:4200")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }
}
