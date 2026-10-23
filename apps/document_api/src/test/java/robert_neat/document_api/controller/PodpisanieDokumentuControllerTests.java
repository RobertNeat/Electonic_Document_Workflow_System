package robert_neat.document_api.controller;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
import robert_neat.document_api.exception.BadRequestException;
import robert_neat.document_api.repository.DokumentRepository;
import robert_neat.document_api.repository.WersjaDokumentuRepository;
import robert_neat.document_api.repository.WpisDziennikaZdarzenRepository;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static robert_neat.document_api.TestJwts.jwtWithRole;

/**
 * Testy orkiestracji POST /api/dokumenty/{id}/podpisz (dms_service + sign_service zamockowane
 * jako @MockitoBean - nie są dostępne w środowisku testowym, zob. DmsServiceClient/SignServiceClient).
 * Seed "demo": d-2 ma aktualną wersję (2) już podpisaną - używane do testu odrzucenia ponownego
 * podpisania; d-1 ma aktualną_wersja=1 bez odpowiadającego wiersza wersja_dokumentu w seedzie -
 * do testu happy-path dokładamy ten wiersz jawnie (niepodpisany) w ramach transakcji testu.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PodpisanieDokumentuControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DokumentRepository dokumentRepository;

    @Autowired
    private WersjaDokumentuRepository wersjaDokumentuRepository;

    @Autowired
    private WpisDziennikaZdarzenRepository wpisDziennikaZdarzenRepository;

    @MockitoBean
    private DmsServiceClient dmsServiceClient;

    @MockitoBean
    private SignServiceClient signServiceClient;

    @Test
    @Transactional
    void podpiszDokument_sukcesTworzyNowaPodpisanaWersje() throws Exception {
        przygotujNiepodpisanaAktualnaWersje("d-1", 1);
        long przedLiczbaAudytu = liczbaWpisowAudytu();

        byte[] oryginalnyPlik = "plik-pdf-przed-podpisem".getBytes();
        byte[] podpisanyPlik = "plik-pdf-po-podpisie".getBytes();
        when(dmsServiceClient.pobierzPlikWersji("d-1", 1)).thenReturn(oryginalnyPlik);
        when(signServiceClient.podpiszPades(eq(oryginalnyPlik), eq("cert-1"), anyString()))
                .thenReturn(new SignServiceClient.WynikPodpisaniaPades(podpisanyPlik, "podpis-123"));
        when(dmsServiceClient.uploadNowaWersja(eq("d-1"), anyString(), eq(podpisanyPlik), anyString()))
                .thenReturn(2);

        mockMvc.perform(post("/api/dokumenty/d-1/podpisz")
                        .with(jwtWithRole("referent")).with(csrf())
                        .contentType("application/json")
                        .content("{\"certyfikatId\":\"cert-1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dokumentId").value("d-1"))
                .andExpect(jsonPath("$.wersja").value(2))
                .andExpect(jsonPath("$.podpisana").value(true))
                .andExpect(jsonPath("$.statusPodpisu").value("poprawny"));

        DokumentEntity dokument = dokumentRepository.findById("d-1").orElseThrow();
        assertEquals(2, dokument.getAktualnaWersja());

        WersjaDokumentuEntity nowaWersja = wersjaDokumentuRepository.findByDokument_IdAndWersja("d-1", 2).orElseThrow();
        assertTrue(nowaWersja.isPodpisana());
        assertEquals("poprawny", nowaWersja.getStatusPodpisu());

        assertEquals(przedLiczbaAudytu + 1, liczbaWpisowAudytu());

        // dokumentReferencja przekazana do sign_service to id NOWEGO wiersza wersja_dokumentu.
        verify(signServiceClient).podpiszPades(eq(oryginalnyPlik), eq("cert-1"), eq(nowaWersja.getId()));
    }

    @Test
    @Transactional
    void podpiszDokument_400GdySignServiceOdrzucaCertyfikat() throws Exception {
        przygotujNiepodpisanaAktualnaWersje("d-1", 1);
        long przedLiczbaWersji = wersjaDokumentuRepository.findByDokument_IdOrderByWersjaDesc("d-1").size();

        byte[] oryginalnyPlik = "plik-pdf-przed-podpisem".getBytes();
        when(dmsServiceClient.pobierzPlikWersji("d-1", 1)).thenReturn(oryginalnyPlik);
        when(signServiceClient.podpiszPades(any(), anyString(), anyString()))
                .thenThrow(new BadRequestException("sign_service odrzucił żądanie podpisania: certyfikat wygasły"));

        mockMvc.perform(post("/api/dokumenty/d-1/podpisz")
                        .with(jwtWithRole("referent")).with(csrf())
                        .contentType("application/json")
                        .content("{\"certyfikatId\":\"cert-wygasly\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.komunikat").exists());

        verify(dmsServiceClient, never()).uploadNowaWersja(anyString(), anyString(), any(), anyString());
        assertEquals(przedLiczbaWersji, wersjaDokumentuRepository.findByDokument_IdOrderByWersjaDesc("d-1").size());
        assertEquals(1, dokumentRepository.findById("d-1").orElseThrow().getAktualnaWersja());
    }

    @Test
    @Transactional
    void podpiszDokument_400GdyAktualnaWersjaJuzPodpisana() throws Exception {
        // d-2 (seed): aktualna_wersja=2, wiersz wd-2 już podpisany.
        mockMvc.perform(post("/api/dokumenty/d-2/podpisz")
                        .with(jwtWithRole("referent")).with(csrf())
                        .contentType("application/json")
                        .content("{\"certyfikatId\":\"cert-1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.komunikat").exists());

        verify(dmsServiceClient, never()).pobierzPlikWersji(anyString(), anyInt());
    }

    @Test
    @Transactional
    void podpiszDokument_404GdyDokumentNieIstnieje() throws Exception {
        mockMvc.perform(post("/api/dokumenty/brak-takiego/podpisz")
                        .with(jwtWithRole("referent")).with(csrf())
                        .contentType("application/json")
                        .content("{\"certyfikatId\":\"cert-1\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void podpiszDokument_403GdyRolaNieuprawniona() throws Exception {
        mockMvc.perform(post("/api/dokumenty/d-1/podpisz")
                        .with(jwtWithRole("ksiegowosc")).with(csrf())
                        .contentType("application/json")
                        .content("{\"certyfikatId\":\"cert-1\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void podpiszDokument_400GdyBrakCertyfikatIdWZadaniu() throws Exception {
        mockMvc.perform(post("/api/dokumenty/d-1/podpisz")
                        .with(jwtWithRole("referent")).with(csrf())
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
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
        assertFalse(wersjaEntity.isPodpisana());
    }

    private long liczbaWpisowAudytu() {
        return wpisDziennikaZdarzenRepository.findAll().stream()
                .filter(w -> "Podpisanie dokumentu".equals(w.getTypOperacji()))
                .count();
    }
}
