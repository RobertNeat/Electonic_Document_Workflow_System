package robert_neat.delivery_mock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import robert_neat.delivery_mock.client.DocumentApiClient;
import robert_neat.delivery_mock.entity.PrzesylkaDostawyEntity;
import robert_neat.delivery_mock.entity.WpisStatusuDostawyEntity;
import robert_neat.delivery_mock.repository.PrzesylkaDostawyRepository;
import robert_neat.delivery_mock.repository.WpisStatusuDostawyRepository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testy panelu Thymeleaf. DocumentApiClient zamockowany - żadna integracja z prawdziwym
 * document_api nie jest uruchamiana w testach tego modułu. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PrzesylkiWidokControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PrzesylkaDostawyRepository przesylkaRepository;

    @Autowired
    private WpisStatusuDostawyRepository wpisRepository;

    @MockitoBean
    private DocumentApiClient documentApiClient;

    /** Przesyłka dedykowana testom symulacji - świeży UUID, żeby nie kolidować z seedem pd-1/pd-2
     * między testami (baza jest współdzielona w ramach kontekstu testowego). */
    private PrzesylkaDostawyEntity noweTestowePrzesylka() {
        OffsetDateTime teraz = OffsetDateTime.now();
        PrzesylkaDostawyEntity entity = new PrzesylkaDostawyEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setNumerReferencyjny("WIDOK-TEST/" + UUID.randomUUID());
        entity.setKanal("operator_pocztowy");
        entity.setStatus("nadane");
        entity.setDataNadania(teraz);
        entity.setDataStatusu(teraz);
        przesylkaRepository.save(entity);

        WpisStatusuDostawyEntity wpis = new WpisStatusuDostawyEntity();
        wpis.setId(UUID.randomUUID().toString());
        wpis.setPrzesylkaDostawyId(entity.getId());
        wpis.setStatus("nadane");
        wpis.setData(teraz);
        wpisRepository.save(wpis);

        return entity;
    }

    @Test
    void lista_renderujeSieIZawieraNumerReferencyjnyZSeeda() throws Exception {
        mockMvc.perform(get("/przesylki"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ADE/2026/00123")));
    }

    @Test
    void szczegoly_renderujeSieZHistoriaStatusow() throws Exception {
        mockMvc.perform(get("/przesylki/{id}", "pd-1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ADE/2026/00123")))
                .andExpect(content().string(containsString("doreczone")));
    }

    @Test
    void symulujStatus_sukces_zapisujeWpisIWolaDocumentApiClient() throws Exception {
        PrzesylkaDostawyEntity przesylka = noweTestowePrzesylka();
        when(documentApiClient.znajdzIdPoNumerzeReferencyjnym(przesylka.getNumerReferencyjny()))
                .thenReturn(Optional.of("p-123"));

        mockMvc.perform(post("/przesylki/{id}/symuluj-status", przesylka.getId())
                        .param("nowyStatus", "odebrane")
                        .param("szczegoly", "Odebrane przez adresata."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/przesylki/*"))
                .andExpect(flash().attributeExists("komunikat"));

        verify(documentApiClient).aktualizujStatus(eq("p-123"), eq("odebrane"));

        PrzesylkaDostawyEntity odswiezona = przesylkaRepository.findById(przesylka.getId()).orElseThrow();
        assertThat(odswiezona.getStatus()).isEqualTo("odebrane");
        assertThat(wpisRepository.findByPrzesylkaDostawyIdOrderByDataAsc(przesylka.getId())).hasSize(2);
    }

    @Test
    void symulujStatus_bladDocumentApi_lokalnaZmianaITakZapisana() throws Exception {
        PrzesylkaDostawyEntity przesylka = noweTestowePrzesylka();
        when(documentApiClient.znajdzIdPoNumerzeReferencyjnym(anyString()))
                .thenThrow(new RuntimeException("document_api niedostępny"));

        mockMvc.perform(post("/przesylki/{id}/symuluj-status", przesylka.getId())
                        .param("nowyStatus", "zwrot"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("ostrzezenieSynchronizacji"));

        PrzesylkaDostawyEntity odswiezona = przesylkaRepository.findById(przesylka.getId()).orElseThrow();
        assertThat(odswiezona.getStatus()).isEqualTo("zwrot");
        assertThat(wpisRepository.findByPrzesylkaDostawyIdOrderByDataAsc(przesylka.getId())).hasSize(2);
    }

    @Test
    void symulujStatus_niezgodnoscPoStronieDocumentApi_lokalnaZmianaITakZapisana() throws Exception {
        PrzesylkaDostawyEntity przesylka = noweTestowePrzesylka();
        when(documentApiClient.znajdzIdPoNumerzeReferencyjnym(anyString())).thenReturn(Optional.of("p-999"));
        doThrow(new RuntimeException("500 od document_api"))
                .when(documentApiClient).aktualizujStatus(eq("p-999"), anyString());

        mockMvc.perform(post("/przesylki/{id}/symuluj-status", przesylka.getId())
                        .param("nowyStatus", "doreczone"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("ostrzezenieSynchronizacji"));

        PrzesylkaDostawyEntity odswiezona = przesylkaRepository.findById(przesylka.getId()).orElseThrow();
        assertThat(odswiezona.getStatus()).isEqualTo("doreczone");
    }
}
