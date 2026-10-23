package robert_neat.document_api.controller;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import robert_neat.document_api.TestcontainersConfiguration;
import robert_neat.document_api.config.PrzesylkiStatusRabbitMqConfig;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static robert_neat.document_api.TestJwts.jwtWithRole;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy integracyjne odbioru zdarzenia zmiany statusu przesyłki z RabbitMQ (Testcontainers).
 * Wiadomości są publikowane jako surowy Message z nagłówkiem __TypeId__ ustawionym na FQN klasy
 * delivery_mock (nie document_api) -
 * odzwierciedla to, co faktycznie trafia na kolejkę w produkcji (dwa różne moduły/classpath'y,
 * różne pakiety Java dla tego samego kontraktu pola-po-polu). Jeśli PrzesylkiStatusRabbitMqConfig
 * miałby zdefiniowany drugi MessageConverter, Boot cicho spadłby na SimpleMessageConverter i ten
 * test by failował na deserializacji - to jest tu też pośrednio sprawdzane.
 * Test NIE jest @Transactional - konsument pracuje na wątku kontenera listenera (asercje muszą
 * odpytywać endpoint w pętli, Awaitility, analogicznie do AkcjeZbioroweControllerTests).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PrzesylkaStatusuConsumerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private static final Duration AWAIT_TIMEOUT = Duration.ofSeconds(15);

    @Test
    void zdarzenieZmianyStatusu_aktualizujePrzesylkeIHistorie() throws Exception {
        NowaPrzesylka przesylka = nadajPismo();

        publikujZdarzenie(przesylka.numerReferencyjny(), "doreczone");

        Awaitility.await().atMost(AWAIT_TIMEOUT).pollInterval(Duration.ofMillis(200)).untilAsserted(() ->
                mockMvc.perform(get("/api/przesylki/{id}", przesylka.id()).with(jwtWithRole("referent")))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("doreczone"))
                        .andExpect(jsonPath("$.historiaStatusow.length()").value(2))
                        .andExpect(jsonPath("$.historiaStatusow[0].status").value("nadane"))
                        .andExpect(jsonPath("$.historiaStatusow[1].status").value("doreczone")));
    }

    @Test
    void zdarzenieZNieznanymNumeremReferencyjnym_jestOdrzucaneBezWplywuNaInnePrzesylki() throws Exception {
        NowaPrzesylka przesylka = nadajPismo();

        // Wiadomość "poison" (nieznany numer referencyjny) - konsument loguje i zwraca (ack), bez
        // ponownego rzucenia, więc nie blokuje przetwarzania kolejnej, poprawnej wiadomości poniżej.
        publikujZdarzenie("brak-takiego-numeru-" + UUID.randomUUID(), "doreczone");
        publikujZdarzenie(przesylka.numerReferencyjny(), "odebrane");

        Awaitility.await().atMost(AWAIT_TIMEOUT).pollInterval(Duration.ofMillis(200)).untilAsserted(() ->
                mockMvc.perform(get("/api/przesylki/{id}", przesylka.id()).with(jwtWithRole("referent")))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("odebrane")));
    }

    private void publikujZdarzenie(String numerReferencyjny, String nowyStatus) {
        String json = "{\"numerReferencyjny\":\"" + numerReferencyjny + "\",\"nowyStatus\":\"" + nowyStatus + "\"}";
        MessageProperties properties = new MessageProperties();
        properties.setContentType("application/json");
        properties.setHeader("__TypeId__", "robert_neat.delivery_mock.dto.ZmianaStatusuPrzesylkiMessage");
        Message message = new Message(json.getBytes(StandardCharsets.UTF_8), properties);
        rabbitTemplate.send(PrzesylkiStatusRabbitMqConfig.EXCHANGE, PrzesylkiStatusRabbitMqConfig.ROUTING_KEY, message);
    }

    /** Nadaje świeże pismo (status początkowy "nadane", numerReferencyjny wygenerowany przez
     * backend) do testowania odbioru zdarzenia - nie mutuje p-1/p-2 z seeda, używanych przez
     * asercje w innych klasach testowych. Kanał "email" (nie "e-doreczenia"), żeby nie kolidować
     * z asercją listaPrzesylek_filtrPoStatusieKanaleKierunku (dokładnie 1 wynik dla
     * e-doreczenia+doreczone+wychodzaca). */
    private NowaPrzesylka nadajPismo() throws Exception {
        Map<String, Object> request = Map.of(
                "sprawaId", "spr-1",
                "dokumentId", "d-2",
                "obywatelId", "ob-1",
                "kanal", "email"
        );
        tools.jackson.databind.ObjectMapper objectMapper = new tools.jackson.databind.ObjectMapper();
        String response = mockMvc.perform(post("/api/przesylki")
                        .with(jwtWithRole("kancelaria")).with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        var node = objectMapper.readTree(response);
        return new NowaPrzesylka(node.get("id").asString(), node.get("numerReferencyjny").asString());
    }

    private record NowaPrzesylka(String id, String numerReferencyjny) {
    }
}
