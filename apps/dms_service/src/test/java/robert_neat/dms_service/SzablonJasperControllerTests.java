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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testy integracyjne REST API szablonów Jasper na realnej bazie Postgres (Testcontainers) i
 * tymczasowym katalogu przechowywania (base-path nadpisany przez @DynamicPropertySource, żeby nie
 * pisać do prawdziwego /data/dms). Seed demo (sz-1/sz-2) nie ma plików na dysku - testy na tych
 * id operują tylko na metadanych, nie na pobieraniu pliku. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class SzablonJasperControllerTests {

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

    @Test
    void listaSzablonow_zawieraSeedoweWpisy() throws Exception {
        mockMvc.perform(get("/api/szablony"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    void getById_404GdyNieIstnieje() throws Exception {
        mockMvc.perform(get("/api/szablony/brak-takiego"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.komunikat").exists());
    }

    @Test
    void getById_seedowyWpis() throws Exception {
        mockMvc.perform(get("/api/szablony/sz-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nazwa").value("decyzja_srodowiskowa.jrxml"))
                .andExpect(jsonPath("$.wersja").value(3));
    }

    @Test
    void uploadSzablon_tworzyWersjeIPlikJestDoPobrania() throws Exception {
        String typDokumentuId = "td-test-" + UUID.randomUUID().toString().substring(0, 8);
        String nazwa = "test_szablon.jrxml";
        MockMultipartFile plik = new MockMultipartFile("plik", nazwa, "application/xml", "<jasperReport/>".getBytes());

        String responseBody = mockMvc.perform(multipart("/api/szablony")
                        .file(plik)
                        .param("typDokumentuId", typDokumentuId)
                        .param("nazwa", nazwa)
                        .param("autor", "Tester"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.wersja").value(1))
                .andExpect(jsonPath("$.typDokumentuId").value(typDokumentuId))
                .andReturn().getResponse().getContentAsString();

        String id = objectMapper.readTree(responseBody).get("id").asString();

        mockMvc.perform(get("/api/szablony/{id}/plik", id))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .bytes("<jasperReport/>".getBytes()));

        // drugi upload tej samej nazwy/typu -> wersja 2 (wersjonowanie szablonów per typDokumentuId+nazwa)
        MockMultipartFile plikV2 = new MockMultipartFile("plik", nazwa, "application/xml", "<jasperReport v2/>".getBytes());
        mockMvc.perform(multipart("/api/szablony")
                        .file(plikV2)
                        .param("typDokumentuId", typDokumentuId)
                        .param("nazwa", nazwa)
                        .param("autor", "Tester"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.wersja").value(2));
    }

    @Test
    void uploadSzablon_odrzucaOryginalnaNazwePlikuZPathTraversal() throws Exception {
        // 'nazwa' (pole formularza) jest bezpieczna - sprawdzamy, że to oryginalna nazwa pliku z
        // uploadu (plik.getOriginalFilename()), realny wektor path traversal, jest walidowana.
        MockMultipartFile plik = new MockMultipartFile("plik", "../../etc/evil.jrxml", "application/xml", "x".getBytes());

        mockMvc.perform(multipart("/api/szablony")
                        .file(plik)
                        .param("typDokumentuId", "td-x")
                        .param("nazwa", "ok.jrxml")
                        .param("autor", "Tester"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pobierzPlik_404GdySeedowyWpisNieMaPlikuNaDysku() throws Exception {
        // Wiersze seeda demo (sz-1/sz-2) mają metadane, ale nie mają realnych plików na wolumenie
        // testowym - pobranie musi zwrócić 404, nie 500.
        mockMvc.perform(get("/api/szablony/sz-1/plik"))
                .andExpect(status().isNotFound());
    }
}
