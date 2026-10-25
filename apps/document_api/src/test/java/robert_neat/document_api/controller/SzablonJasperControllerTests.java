package robert_neat.document_api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.document_api.TestcontainersConfiguration;
import robert_neat.document_api.client.DmsServiceClient;
import robert_neat.document_api.exception.BadRequestException;
import robert_neat.document_api.repository.SzablonJasperRepository;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static robert_neat.document_api.TestJwts.jwtWithRole;

/** Testy uploadu szablonu Jasper (POST /api/szablony) - proxy do dms_service, zob.
 * DokumentService.wgrajSzablon. DmsServiceClient jest mockowany, więc testy nie zależą od realnego
 * dms_service - sprawdzają tylko orkiestrację i zapis lokalny w core.szablon_jasper. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class SzablonJasperControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SzablonJasperRepository szablonJasperRepository;

    @MockitoBean
    private DmsServiceClient dmsServiceClient;

    @Test
    @Transactional
    void wgrajSzablon_sukces_zapisujeLokalnyWiersz() throws Exception {
        when(dmsServiceClient.uploadSzablon(eq("td-3"), eq("nowy_szablon.jrxml"), anyString(), any(), eq("nowy_szablon.jrxml")))
                .thenReturn(new DmsServiceClient.SzablonJasperDms(
                        "sz-test-1", "td-3", "nowy_szablon.jrxml", 1, LocalDate.of(2026, 10, 25), "Testowy Administrator"));

        MockMultipartFile plik = new MockMultipartFile(
                "plik", "nowy_szablon.jrxml", "application/xml", "<jasperReport/>".getBytes());

        mockMvc.perform(multipart("/api/szablony")
                        .file(plik)
                        .param("typDokumentuId", "td-3")
                        .with(jwtWithRole("administrator")).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("sz-test-1"))
                .andExpect(jsonPath("$.typDokumentuId").value("td-3"))
                .andExpect(jsonPath("$.nazwa").value("nowy_szablon.jrxml"))
                .andExpect(jsonPath("$.wersja").value(1));

        assertTrue(szablonJasperRepository.findById("sz-test-1").isPresent());
        assertEquals("td-3", szablonJasperRepository.findById("sz-test-1").orElseThrow().getTypDokumentu().getId());
    }

    @Test
    @Transactional
    void wgrajSzablon_403GdyRolaNieuprawniona() throws Exception {
        MockMultipartFile plik = new MockMultipartFile(
                "plik", "nowy_szablon.jrxml", "application/xml", "<jasperReport/>".getBytes());

        mockMvc.perform(multipart("/api/szablony")
                        .file(plik)
                        .param("typDokumentuId", "td-3")
                        .with(jwtWithRole("referent")).with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(dmsServiceClient);
    }

    @Test
    @Transactional
    void wgrajSzablon_404GdyTypDokumentuNieIstnieje() throws Exception {
        MockMultipartFile plik = new MockMultipartFile(
                "plik", "nowy_szablon.jrxml", "application/xml", "<jasperReport/>".getBytes());

        mockMvc.perform(multipart("/api/szablony")
                        .file(plik)
                        .param("typDokumentuId", "brak-takiego")
                        .with(jwtWithRole("administrator")).with(csrf()))
                .andExpect(status().isNotFound());

        verifyNoInteractions(dmsServiceClient);
    }

    @Test
    @Transactional
    void wgrajSzablon_400GdyDmsServiceOdrzuciPlik() throws Exception {
        when(dmsServiceClient.uploadSzablon(eq("td-3"), anyString(), anyString(), any(), anyString()))
                .thenThrow(new BadRequestException("dms_service odrzucił upload szablonu: rozszerzenie musi być .jrxml"));

        MockMultipartFile plik = new MockMultipartFile(
                "plik", "nie_jrxml.txt", "text/plain", "nie jrxml".getBytes());

        mockMvc.perform(multipart("/api/szablony")
                        .file(plik)
                        .param("typDokumentuId", "td-3")
                        .with(jwtWithRole("administrator")).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.komunikat").exists());
    }
}
