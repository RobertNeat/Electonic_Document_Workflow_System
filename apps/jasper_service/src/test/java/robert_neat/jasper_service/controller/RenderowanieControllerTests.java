package robert_neat.jasper_service.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import robert_neat.jasper_service.client.DmsServiceClient;
import robert_neat.jasper_service.dto.SzablonMetadane;
import robert_neat.jasper_service.exception.NotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RenderowanieControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DmsServiceClient dmsServiceClient;

    private static byte[] wczytajTestowyJrxml() throws IOException {
        try (InputStream in = RenderowanieControllerTests.class.getClassLoader()
                .getResourceAsStream("testowy-szablon.jrxml")) {
            return in.readAllBytes();
        }
    }

    private static SzablonMetadane metadane(String id) {
        return new SzablonMetadane(id, "typ-1", "testowy-szablon.jrxml", 1, LocalDate.now(), "autor-test");
    }

    @Test
    void renderujZwracaPdfDlaPoprawnegoSzablonu() throws Exception {
        when(dmsServiceClient.pobierzMetadaneSzablonu("szablon-1")).thenReturn(metadane("szablon-1"));
        when(dmsServiceClient.pobierzPlikSzablonu("szablon-1")).thenReturn(wczytajTestowyJrxml());

        byte[] body = mockMvc.perform(post("/api/render/szablon-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tytul\":\"Testowy tytul\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(body).isNotEmpty();
        assertThat(new String(body, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }

    @Test
    void renderujAkceptujePustyJsonBody() throws Exception {
        when(dmsServiceClient.pobierzMetadaneSzablonu("szablon-1")).thenReturn(metadane("szablon-1"));
        when(dmsServiceClient.pobierzPlikSzablonu("szablon-1")).thenReturn(wczytajTestowyJrxml());

        mockMvc.perform(post("/api/render/szablon-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    @Test
    void renderujZwraca404GdySzablonNieIstnieje() throws Exception {
        when(dmsServiceClient.pobierzMetadaneSzablonu("brak")).thenThrow(new NotFoundException("Szablon o id brak nie istnieje w dms_service"));

        mockMvc.perform(post("/api/render/brak")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void renderujZwraca400DlaUszkodzonegoSzablonu() throws Exception {
        when(dmsServiceClient.pobierzMetadaneSzablonu("uszkodzony")).thenReturn(metadane("uszkodzony"));
        when(dmsServiceClient.pobierzPlikSzablonu("uszkodzony"))
                .thenReturn("to nie jest poprawny jrxml".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(post("/api/render/uszkodzony")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
