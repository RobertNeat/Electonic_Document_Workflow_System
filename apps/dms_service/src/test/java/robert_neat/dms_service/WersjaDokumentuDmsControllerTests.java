package robert_neat.dms_service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testy integracyjne REST API wersji dokumentów DMS. Każdy test wersjonowania używa świeżego,
 * losowego dokumentId, żeby nie kolidować z seedem demo (d-2/wd-1/wd-2) ani między testami. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class WersjaDokumentuDmsControllerTests {

    @TempDir
    static Path tempDir;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void overrideStorage(DynamicPropertyRegistry registry) {
        registry.add("dms.storage.base-path", () -> tempDir.toString());
    }

    private static String noweDokumentId() {
        return "doc" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    @Test
    void listaWersji_pustaListaDlaNieznanegoDokumentu() throws Exception {
        mockMvc.perform(get("/api/dokumenty/{id}/wersje", noweDokumentId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getWersja_404GdyNieIstnieje() throws Exception {
        mockMvc.perform(get("/api/dokumenty/{id}/wersje/{w}", noweDokumentId(), 1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.komunikat").exists());
    }

    @Test
    void uploadWersja_pierwszaWersjaNumer1IPlikDoPobrania() throws Exception {
        String dokumentId = noweDokumentId();
        MockMultipartFile plik = new MockMultipartFile("plik", "pismo.pdf", "application/pdf", "treść pisma".getBytes());

        mockMvc.perform(multipart("/api/dokumenty/{id}/wersje", dokumentId)
                        .file(plik)
                        .param("autor", "Tester"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.wersja").value(1))
                .andExpect(jsonPath("$.dokumentId").value(dokumentId))
                .andExpect(jsonPath("$.podpisana").value(false))
                .andExpect(jsonPath("$.statusPodpisu").value("brak"));

        mockMvc.perform(get("/api/dokumenty/{id}/wersje/{w}/plik", dokumentId, 1))
                .andExpect(status().isOk())
                .andExpect(content().bytes("treść pisma".getBytes()));
    }

    @Test
    void uploadWersja_kolejnyUploadZwiekszaWersje() throws Exception {
        String dokumentId = noweDokumentId();
        MockMultipartFile v1 = new MockMultipartFile("plik", "a.pdf", "application/pdf", "v1".getBytes());
        MockMultipartFile v2 = new MockMultipartFile("plik", "a.pdf", "application/pdf", "v2".getBytes());

        mockMvc.perform(multipart("/api/dokumenty/{id}/wersje", dokumentId).file(v1).param("autor", "Tester"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.wersja").value(1));

        mockMvc.perform(multipart("/api/dokumenty/{id}/wersje", dokumentId).file(v2).param("autor", "Tester"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.wersja").value(2));

        mockMvc.perform(get("/api/dokumenty/{id}/wersje", dokumentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].wersja").value(1))
                .andExpect(jsonPath("$[1].wersja").value(2));
    }

    @Test
    void uploadWersja_odrzucaDokumentIdZPathTraversal() throws Exception {
        // ".." i "/" w pełnym segmencie są znormalizowane przez kontener serwletów przed dotarciem
        // do kontrolera (inny route -> 404) - tu testujemy wartość przechodzącą routing, ale wciąż
        // niedozwoloną wg białej listy znaków segmentu (SAFE_SEGMENT) w FileStorageService.waliduj.
        MockMultipartFile plik = new MockMultipartFile("plik", "a.pdf", "application/pdf", "x".getBytes());

        mockMvc.perform(multipart("/api/dokumenty/{id}/wersje", "a..b").file(plik).param("autor", "Tester"))
                .andExpect(status().isBadRequest());
    }
}
