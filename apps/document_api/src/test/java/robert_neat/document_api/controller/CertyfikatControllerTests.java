package robert_neat.document_api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import robert_neat.document_api.TestcontainersConfiguration;
import robert_neat.document_api.client.SignServiceClient;
import robert_neat.document_api.dto.dokumenty.Certyfikat;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static robert_neat.document_api.TestJwts.jwtWithRole;

/** Testy GET /api/certyfikaty - proxy do sign_service (zamockowany, zob. SignServiceClient). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class CertyfikatControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SignServiceClient signServiceClient;

    @Test
    void listaCertyfikatow_zwracaWszystkieCertyfikatyZSignService() throws Exception {
        when(signServiceClient.listaCertyfikatow()).thenReturn(List.of(
                new Certyfikat("cert-1", "CN=Katarzyna Zielińska, O=Urząd, C=PL", "7A3F1B2C9D0E4455",
                        LocalDate.of(2026, 1, 10), LocalDate.of(2027, 1, 10), "aktywny", "Demo CA Issuing"),
                new Certyfikat("cert-2", "CN=Jan Kowalski, O=Urząd, C=PL", "1122334455667788",
                        LocalDate.of(2020, 1, 10), LocalDate.of(2021, 1, 10), "wygasly", "Demo CA Issuing")));

        mockMvc.perform(get("/api/certyfikaty").with(jwtWithRole("referent")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("cert-1"))
                .andExpect(jsonPath("$[0].status").value("aktywny"))
                .andExpect(jsonPath("$[1].status").value("wygasly"));
    }
}
