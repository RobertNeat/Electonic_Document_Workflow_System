package robert_neat.document_api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.document_api.TestcontainersConfiguration;
import robert_neat.document_api.client.DmsServiceClient;
import robert_neat.document_api.client.SignServiceClient;
import robert_neat.document_api.entity.DokumentEntity;
import robert_neat.document_api.entity.WersjaDokumentuEntity;
import robert_neat.document_api.repository.DokumentRepository;
import robert_neat.document_api.repository.WersjaDokumentuRepository;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static robert_neat.document_api.TestJwts.jwtWithRole;

/**
 * Testy GET /api/dokumenty/{id}/wersje/{wersja}/podpis (dms_service + sign_service zamockowane
 * jako @MockitoBean - nie są dostępne w środowisku testowym). Seed "demo": d-2/wd-2 ma wersję 2
 * już podpisaną - używane do happy-path; d-1 ma aktualną_wersja=1 bez odpowiadającego wiersza
 * wersja_dokumentu w seedzie - używane do testu 404 na brakującej wersji.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class SzczegolyPodpisuControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DokumentRepository dokumentRepository;

    @Autowired
    private WersjaDokumentuRepository wersjaDokumentuRepository;

    @MockitoBean
    private DmsServiceClient dmsServiceClient;

    @MockitoBean
    private SignServiceClient signServiceClient;

    @Test
    @Transactional
    void szczegolyPodpisu_200ZwracaSzczegolyZeZnacznikiemTsaNull() throws Exception {
        byte[] plik = "podpisany-pdf".getBytes();
        when(dmsServiceClient.pobierzPlikWersji("d-2", 2)).thenReturn(plik);
        when(signServiceClient.weryfikujPades(plik)).thenReturn(new SignServiceClient.WynikWeryfikacji(
                "poprawny", "CN=Jan Kowalski", "2026-01-10T12:00:00Z", "abc123",
                "nieznany", "nieznany", "brak"));

        mockMvc.perform(get("/api/dokumenty/d-2/wersje/2/podpis")
                        .with(jwtWithRole("referent")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.podpisujacy").value("CN=Jan Kowalski"))
                .andExpect(jsonPath("$.czasPodpisu").value("2026-01-10T12:00:00Z"))
                .andExpect(jsonPath("$.odciskCertyfikatu").value("abc123"))
                .andExpect(jsonPath("$.statusOcsp").value("nieznany"))
                .andExpect(jsonPath("$.statusCrl").value("nieznany"))
                .andExpect(jsonPath("$.znacznikCzasuTsa").value(nullValue()));
    }

    @Test
    @Transactional
    void szczegolyPodpisu_200NiezaleznieOdRoli() throws Exception {
        byte[] plik = "podpisany-pdf".getBytes();
        when(dmsServiceClient.pobierzPlikWersji("d-2", 2)).thenReturn(plik);
        when(signServiceClient.weryfikujPades(plik)).thenReturn(new SignServiceClient.WynikWeryfikacji(
                "poprawny", "CN=Jan Kowalski", "2026-01-10T12:00:00Z", "abc123",
                "nieznany", "nieznany", "2026-01-10T12:05:00Z"));

        mockMvc.perform(get("/api/dokumenty/d-2/wersje/2/podpis")
                        .with(jwtWithRole("ksiegowosc")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.znacznikCzasuTsa").value("2026-01-10T12:05:00Z"));
    }

    @Test
    @Transactional
    void szczegolyPodpisu_400GdyWersjaNiePodpisana() throws Exception {
        przygotujNiepodpisanaAktualnaWersje("d-1", 1);

        mockMvc.perform(get("/api/dokumenty/d-1/wersje/1/podpis")
                        .with(jwtWithRole("referent")).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.komunikat").exists());

        verify(dmsServiceClient, never()).pobierzPlikWersji(anyString(), anyInt());
        verify(signServiceClient, never()).weryfikujPades(any());
    }

    @Test
    @Transactional
    void szczegolyPodpisu_404GdyDokumentNieIstnieje() throws Exception {
        mockMvc.perform(get("/api/dokumenty/brak-takiego/wersje/1/podpis")
                        .with(jwtWithRole("referent")).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void szczegolyPodpisu_404GdyWersjaNieIstnieje() throws Exception {
        // d-1 istnieje, ale w seedzie nie ma wiersza wersja_dokumentu dla jego aktualnej wersji (1).
        mockMvc.perform(get("/api/dokumenty/d-1/wersje/1/podpis")
                        .with(jwtWithRole("referent")).with(csrf()))
                .andExpect(status().isNotFound());
    }

    private void przygotujNiepodpisanaAktualnaWersje(String dokumentId, int wersja) {
        DokumentEntity dokument = dokumentRepository.findById(dokumentId).orElseThrow();
        WersjaDokumentuEntity wersjaEntity = new WersjaDokumentuEntity();
        wersjaEntity.setId(UUID.randomUUID().toString());
        wersjaEntity.setDokument(dokument);
        wersjaEntity.setWersja(wersja);
        wersjaEntity.setPodpisana(false);
        wersjaEntity.setDataUtworzenia(LocalDate.now());
        wersjaEntity.setAutor("Test Autor");
        wersjaEntity.setStatusPodpisu("brak");
        wersjaDokumentuRepository.save(wersjaEntity);
        wersjaDokumentuRepository.flush();
    }
}
