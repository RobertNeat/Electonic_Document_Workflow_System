package robert_neat.document_api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.document_api.TestcontainersConfiguration;
import robert_neat.document_api.client.DmsServiceClient;
import robert_neat.document_api.exception.NotFoundException;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static robert_neat.document_api.TestJwts.jwtWithRole;

/**
 * Testy GET /api/dokumenty/{id}/wersje/{wersja}/plik (proxy do dms_service, zamockowanego jako
 * @MockitoBean - nie jest dostępny w środowisku testowym). Seed "demo": d-1 istnieje w bazie
 * document_api (metadane), niezależnie od tego, czy dms_service faktycznie ma plik danej wersji.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PobranieplikuWersjiControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DmsServiceClient dmsServiceClient;

    @Test
    @Transactional
    void pobierzPlikWersji_200ZwracaBajtyIContentTypePdf() throws Exception {
        byte[] plik = "plik-pdf-przykladowy".getBytes();
        when(dmsServiceClient.pobierzPlikWersji("d-1", 1)).thenReturn(plik);

        mockMvc.perform(get("/api/dokumenty/d-1/wersje/1/plik")
                        .with(jwtWithRole("referent")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(plik))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("inline")));
    }

    @Test
    @Transactional
    void pobierzPlikWersji_200NiezaleznieOdRoli() throws Exception {
        byte[] plik = "plik-pdf-przykladowy".getBytes();
        when(dmsServiceClient.pobierzPlikWersji("d-1", 1)).thenReturn(plik);

        mockMvc.perform(get("/api/dokumenty/d-1/wersje/1/plik")
                        .with(jwtWithRole("ksiegowosc")).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    void pobierzPlikWersji_404GdyDokumentNieIstnieje() throws Exception {
        mockMvc.perform(get("/api/dokumenty/brak-takiego/wersje/1/plik")
                        .with(jwtWithRole("referent")).with(csrf()))
                .andExpect(status().isNotFound());

        verify(dmsServiceClient, never()).pobierzPlikWersji(anyString(), anyInt());
    }

    @Test
    @Transactional
    void pobierzPlikWersji_404GdyDmsServiceNieMaPliku() throws Exception {
        when(dmsServiceClient.pobierzPlikWersji("d-1", 99))
                .thenThrow(new NotFoundException("Plik wersji 99 dokumentu d-1 nie istnieje w dms_service"));

        mockMvc.perform(get("/api/dokumenty/d-1/wersje/99/plik")
                        .with(jwtWithRole("referent")).with(csrf()))
                .andExpect(status().isNotFound());
    }
}
