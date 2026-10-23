package robert_neat.sign_service;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.Pkcs12SignatureToken;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import robert_neat.sign_service.config.DemoPkiInitializer;
import robert_neat.sign_service.config.PkiProperties;
import robert_neat.sign_service.entity.CertyfikatEntity;
import robert_neat.sign_service.entity.DemoCaEntity;
import robert_neat.sign_service.repository.CertyfikatRepository;
import robert_neat.sign_service.repository.DemoCaRepository;
import robert_neat.sign_service.repository.WystawionyPodpisRepository;
import robert_neat.sign_service.service.DemoCertGenerator;
import robert_neat.sign_service.service.PodpisService;
import robert_neat.sign_service.service.WeryfikacjaService;

import java.io.ByteArrayOutputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testy integracyjne podpisywania/weryfikacji PAdES BASELINE-B (Slice 1 - bez TSA/OCSP/CRL
 * sieciowego). Seed demo: cert-1 aktywny, cert-2 wygasly, cert-3 uniewazniony (zob. 100-seed.xml). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PodpisWeryfikacjaTests {

    private static final char[] KEYSTORE_PASSWORD = "demo-keystore-password".toCharArray();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CertyfikatRepository certyfikatRepository;

    @Autowired
    private WystawionyPodpisRepository wystawionyPodpisRepository;

    @Autowired
    private DemoCaRepository demoCaRepository;

    @Autowired
    private PodpisService podpisService;

    @Autowired
    private WeryfikacjaService weryfikacjaService;

    @Autowired
    private DemoPkiInitializer demoPkiInitializer;

    @Autowired
    private PkiProperties pkiProperties;

    @Autowired
    private DemoCertGenerator demoCertGenerator;

    private static byte[] prostyPdf() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.save(out);
        }
        return out.toByteArray();
    }

    @Test
    void podpisaniePadesCertyfikatemAktywnym_sukcesZWpisemWystawionegoPodpisu() throws Exception {
        byte[] pdf = prostyPdf();
        MockMultipartFile plik = new MockMultipartFile("plik", "dok.pdf", "application/pdf", pdf);

        var wynikMvc = mockMvc.perform(multipart("/api/podpisy/pades")
                        .file(plik)
                        .param("certyfikatId", "cert-1")
                        .param("dokumentReferencja", "wd-test-1"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Wystawiony-Podpis-Id"))
                .andReturn();

        byte[] podpisanyPdf = wynikMvc.getResponse().getContentAsByteArray();
        assertThat(new String(podpisanyPdf, 0, 4)).isEqualTo("%PDF");

        String wystawionyPodpisId = wynikMvc.getResponse().getHeader("X-Wystawiony-Podpis-Id");
        var wystawionyPodpis = wystawionyPodpisRepository.findById(wystawionyPodpisId).orElseThrow();
        assertThat(wystawionyPodpis.getCertyfikat().getId()).isEqualTo("cert-1");
        assertThat(wystawionyPodpis.getFormat()).isEqualTo("pades");
        assertThat(wystawionyPodpis.getDokumentReferencja()).isEqualTo("wd-test-1");
    }

    @Test
    void podpisanie_odrzuconeDlaCertyfikatuWygaslego() throws Exception {
        byte[] pdf = prostyPdf();
        MockMultipartFile plik = new MockMultipartFile("plik", "dok.pdf", "application/pdf", pdf);

        mockMvc.perform(multipart("/api/podpisy/pades")
                        .file(plik)
                        .param("certyfikatId", "cert-2")
                        .param("dokumentReferencja", "wd-test-2"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void podpisanie_odrzuconeDlaCertyfikatuUniewaznionego() throws Exception {
        byte[] pdf = prostyPdf();
        MockMultipartFile plik = new MockMultipartFile("plik", "dok.pdf", "application/pdf", pdf);

        mockMvc.perform(multipart("/api/podpisy/pades")
                        .file(plik)
                        .param("certyfikatId", "cert-3")
                        .param("dokumentReferencja", "wd-test-3"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void weryfikacja_podpisZlozonyCertem1_statusPoprawny() throws Exception {
        byte[] pdf = prostyPdf();
        MockMultipartFile plikDoPodpisania = new MockMultipartFile("plik", "dok.pdf", "application/pdf", pdf);

        var wynikMvc = mockMvc.perform(multipart("/api/podpisy/pades")
                        .file(plikDoPodpisania)
                        .param("certyfikatId", "cert-1")
                        .param("dokumentReferencja", "wd-test-weryfikacja-1"))
                .andExpect(status().isOk())
                .andReturn();
        byte[] podpisanyPdf = wynikMvc.getResponse().getContentAsByteArray();

        MockMultipartFile plikDoWeryfikacji = new MockMultipartFile("plik", "podpisany.pdf", "application/pdf", podpisanyPdf);
        mockMvc.perform(multipart("/api/podpisy/weryfikacja").file(plikDoWeryfikacji))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("poprawny"));
    }

    /**
     * cert-3 jest uniewazniony - endpoint /api/podpisy/pades go odrzuca (400), wiec testujemy
     * weryfikacje osobno zbudowanego podpisu, obchodzac blokade w PodpisService przez bezposrednie
     * uzycie materialu kryptograficznego wygenerowanego przez DemoPkiInitializer dla cert-3.
     */
    @Test
    void weryfikacja_podpisCertem3Uniewaznionym_statusCertyfikatUniewazniony() throws Exception {
        CertyfikatEntity cert3 = certyfikatRepository.findById("cert-3").orElseThrow();
        assertThat(cert3.getKeystorePkcs12()).isNotNull();

        byte[] pdf = prostyPdf();
        byte[] podpisanyPdf = podpiszZKeystore(cert3.getKeystorePkcs12(), pdf);

        MockMultipartFile plikDoWeryfikacji = new MockMultipartFile("plik", "podpisany.pdf", "application/pdf", podpisanyPdf);
        mockMvc.perform(multipart("/api/podpisy/weryfikacja").file(plikDoWeryfikacji))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("certyfikat_uniewazniony"));
    }

    /**
     * Certyfikat podpisany przez OBCE (nie demo-CA) samopodpisane CA, ale z numerem seryjnym
     * podstawionym na numer_seryjny certyfikatu cert-1 - bez sprawdzenia lancucha do demo-CA jako
     * trust anchor, serwis blednie zaakceptowalby ten podpis jako "poprawny" dla cert-1.
     */
    @Test
    void weryfikacja_certyfikatPodstawionyZObcegoCa_statusNiepoprawny() throws Exception {
        CertyfikatEntity cert1 = certyfikatRepository.findById("cert-1").orElseThrow();

        KeyPair obceCaKeyPair = demoCertGenerator.generateKeyPair();
        X509Certificate obceCaCert = demoCertGenerator.generateCaCertificate(obceCaKeyPair, "CN=Obce CA,O=Atakujacy,C=PL");

        KeyPair podstawionyLeafKeyPair = demoCertGenerator.generateKeyPair();
        X509Certificate podstawionyLeafCert = demoCertGenerator.generateLeafCertificate(
                obceCaKeyPair, obceCaCert, podstawionyLeafKeyPair,
                cert1.getNumerSeryjny(), cert1.getPodmiot(),
                LocalDate.now().minusDays(1), LocalDate.now().plusYears(1));

        byte[] podstawionyKeystore = demoCertGenerator.buildPkcs12(
                "podstawiony", podstawionyLeafKeyPair.getPrivate(), KEYSTORE_PASSWORD, podstawionyLeafCert, obceCaCert);

        byte[] pdf = prostyPdf();
        byte[] podpisanyPdf = podpiszZKeystore(podstawionyKeystore, pdf);

        MockMultipartFile plikDoWeryfikacji = new MockMultipartFile("plik", "podpisany.pdf", "application/pdf", podpisanyPdf);
        mockMvc.perform(multipart("/api/podpisy/weryfikacja").file(plikDoWeryfikacji))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("niepoprawny"));
    }

    @Test
    void weryfikacja_niepoprawnyPlikPdf_400() throws Exception {
        MockMultipartFile smieciowyPlik = new MockMultipartFile("plik", "smieci.pdf", "application/pdf", "to nie jest pdf".getBytes());
        mockMvc.perform(multipart("/api/podpisy/weryfikacja").file(smieciowyPlik))
                .andExpect(status().isBadRequest());
    }

    private byte[] podpiszZKeystore(byte[] keystorePkcs12, byte[] pdf) throws Exception {
        try (Pkcs12SignatureToken token = new Pkcs12SignatureToken(
                keystorePkcs12, new KeyStore.PasswordProtection(KEYSTORE_PASSWORD))) {
            List<DSSPrivateKeyEntry> keys = token.getKeys();
            DSSPrivateKeyEntry privateKeyEntry = keys.get(0);
            var certificateVerifier = PodpisService.buildOfflineCertificateVerifier();
            PAdESService service = new PAdESService(certificateVerifier);
            PAdESSignatureParameters parameters = new PAdESSignatureParameters();
            parameters.setSignatureLevel(SignatureLevel.PAdES_BASELINE_B);
            parameters.setDigestAlgorithm(DigestAlgorithm.SHA256);
            parameters.setSigningCertificate(privateKeyEntry.getCertificate());
            parameters.setCertificateChain(privateKeyEntry.getCertificateChain());

            DSSDocument toSignDocument = new InMemoryDocument(pdf, "dokument.pdf");
            ToBeSigned dataToSign = service.getDataToSign(toSignDocument, parameters);
            SignatureValue signatureValue = token.sign(dataToSign, parameters.getDigestAlgorithm(), privateKeyEntry);
            DSSDocument signedDocument = service.signDocument(toSignDocument, parameters, signatureValue);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            signedDocument.writeTo(out);
            return out.toByteArray();
        }
    }

    @Test
    void inicjalizatorPki_idempotentny_niePowielaMaterialu() throws Exception {
        byte[] demoCaDerPrzed = demoCaRepository.findAll().get(0).getCertyfikatDer();
        long liczbaDemoCaPrzed = demoCaRepository.count();
        byte[][] certDerPrzed = certyfikatDerDlaWszystkich();
        byte[][] keystorePrzed = keystorePkcs12DlaWszystkich();

        demoPkiInitializer.run(null);

        assertThat(demoCaRepository.count()).isEqualTo(liczbaDemoCaPrzed).isEqualTo(1);
        assertThat(demoCaRepository.findAll().get(0).getCertyfikatDer()).isEqualTo(demoCaDerPrzed);

        byte[][] certDerPo = certyfikatDerDlaWszystkich();
        byte[][] keystorePo = keystorePkcs12DlaWszystkich();
        for (int i = 0; i < certDerPrzed.length; i++) {
            assertThat(certDerPo[i]).isEqualTo(certDerPrzed[i]);
            assertThat(keystorePo[i]).isEqualTo(keystorePrzed[i]);
        }
    }

    private static final String[] CERT_IDS = {"cert-1", "cert-2", "cert-3"};

    private byte[][] certyfikatDerDlaWszystkich() {
        byte[][] result = new byte[CERT_IDS.length][];
        for (int i = 0; i < CERT_IDS.length; i++) {
            result[i] = certyfikatRepository.findById(CERT_IDS[i]).orElseThrow().getCertyfikatDer();
        }
        return result;
    }

    private byte[][] keystorePkcs12DlaWszystkich() {
        byte[][] result = new byte[CERT_IDS.length][];
        for (int i = 0; i < CERT_IDS.length; i++) {
            result[i] = certyfikatRepository.findById(CERT_IDS[i]).orElseThrow().getKeystorePkcs12();
        }
        return result;
    }
}
