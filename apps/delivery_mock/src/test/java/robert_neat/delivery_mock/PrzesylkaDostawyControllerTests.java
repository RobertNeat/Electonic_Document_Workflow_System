package robert_neat.delivery_mock;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testy REST API delivery_mock (punkt 2 zakresu). RabbitTemplate zamockowany - nie jest
 * wołany przez ten kontroler, ale mockujemy go wszędzie w testach tego modułu, żeby wszystkie
 * klasy testowe dzieliły ten sam kontekst Springa (ten sam cache key = jeden kontener Postgresa). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PrzesylkaDostawyControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @Test
    void listaWszystkich_zawieraDaneZSeeda() throws Exception {
        mockMvc.perform(get("/api/przesylki-dostawy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.numerReferencyjny == 'ADE/2026/00123')]").exists())
                .andExpect(jsonPath("$[?(@.numerReferencyjny == 'PL2026000998')]").exists());
    }

    @Test
    void getById_404GdyNieIstnieje() throws Exception {
        mockMvc.perform(get("/api/przesylki-dostawy/{id}", "brak-takiego-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.komunikat").exists());
    }

    @Test
    void getById_zwracaSzczegolyIHistorieDlaSeeda() throws Exception {
        mockMvc.perform(get("/api/przesylki-dostawy/{id}", "pd-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.przesylka.id").value("pd-1"))
                .andExpect(jsonPath("$.historiaStatusow.length()").value(2));
    }

    @Test
    void rejestrujPrzesylke_sukces() throws Exception {
        String numerReferencyjny = "TEST/" + System.nanoTime();
        String requestBody = "{\"numerReferencyjny\":\"" + numerReferencyjny + "\",\"kanal\":\"email\"}";

        mockMvc.perform(post("/api/przesylki-dostawy")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numerReferencyjny").value(numerReferencyjny))
                .andExpect(jsonPath("$.kanal").value("email"))
                .andExpect(jsonPath("$.status").value("nadane"));
    }

    @Test
    void rejestrujPrzesylke_400DlaDuplikatuNumeruReferencyjnego() throws Exception {
        String requestBody = "{\"numerReferencyjny\":\"ADE/2026/00123\",\"kanal\":\"e-doreczenia\"}";

        mockMvc.perform(post("/api/przesylki-dostawy")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }
}
