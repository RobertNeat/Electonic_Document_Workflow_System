package robert_neat.sign_service;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SignaturePackaging;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.spi.x509.tsp.KeyEntityTSPSource;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.Pkcs12SignatureToken;
import eu.europa.esig.dss.xades.XAdESSignatureParameters;
import eu.europa.esig.dss.xades.signature.XAdESService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import robert_neat.sign_service.entity.CertyfikatEntity;
import robert_neat.sign_service.entity.DemoTsaEntity;
import robert_neat.sign_service.repository.CertyfikatRepository;
import robert_neat.sign_service.repository.DemoTsaRepository;
import robert_neat.sign_service.repository.WystawionyPodpisRepository;
import robert_neat.sign_service.service.DemoCertGenerator;
import robert_neat.sign_service.service.PodpisService;

import java.io.ByteArrayOutputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testy integracyjne podpisywania/weryfikacji XAdES BASELINE-T - mirror PodpisWeryfikacjaTests
 * (PAdES), ten sam seed demo: cert-1 aktywny, cert-2 wygasly, cert-3 uniewazniony. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class XadesPodpisWeryfikacjaTests {

    private static final char[] KEYSTORE_PASSWORD = "demo-keystore-password".toCharArray();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CertyfikatRepository certyfikatRepository;

    @Autowired
    private WystawionyPodpisRepository wystawionyPodpisRepository;

    @Autowired
    private DemoTsaRepository demoTsaRepository;

    @Autowired
    private DemoCertGenerator demoCertGenerator;

    private static byte[] prostyXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><dokument><tresc>test</tresc></dokument>"
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Test
    void podpisanieXadesCertyfikatemAktywnym_sukcesZWpisemWystawionegoPodpisu() throws Exception {
        byte[] xml = prostyXml();
        MockMultipartFile plik = new MockMultipartFile("plik", "dok.xml", "application/xml", xml);

        var wynikMvc = mockMvc.perform(multipart("/api/podpisy/xades")
                        .file(plik)
                        .param("certyfikatId", "cert-1")
                        .param("dokumentReferencja", "wd-test-xades-1"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Wystawiony-Podpis-Id"))
                .andReturn();

        byte[] podpisanyXml = wynikMvc.getResponse().getContentAsByteArray();
        assertThat(new String(podpisanyXml, java.nio.charset.StandardCharsets.UTF_8)).contains("<ds:Signature");

        String wystawionyPodpisId = wynikMvc.getResponse().getHeader("X-Wystawiony-Podpis-Id");
        var wystawionyPodpis = wystawionyPodpisRepository.findById(wystawionyPodpisId).orElseThrow();
        assertThat(wystawionyPodpis.getCertyfikat().getId()).isEqualTo("cert-1");
        assertThat(wystawionyPodpis.getFormat()).isEqualTo("xades");
        assertThat(wystawionyPodpis.getDokumentReferencja()).isEqualTo("wd-test-xades-1");

        // Znacznik czasu rzeczywisty (BASELINE-T) - parsowalny jako Instant.
        Instant znacznikCzasu = Instant.parse(wystawionyPodpis.getZnacznikCzasu());

        MockMultipartFile plikDoWeryfikacji = new MockMultipartFile("plik", "podpisany.xml", "application/xml", podpisanyXml);
        mockMvc.perform(multipart("/api/podpisy/weryfikacja-xades").file(plikDoWeryfikacji))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("poprawny"))
                .andExpect(jsonPath("$.znacznikCzasuTsa").value(znacznikCzasu.toString()));
    }

    @Test
    void podpisanieXades_odrzuconeDlaCertyfikatuWygaslego() throws Exception {
        byte[] xml = prostyXml();
        MockMultipartFile plik = new MockMultipartFile("plik", "dok.xml", "application/xml", xml);

        mockMvc.perform(multipart("/api/podpisy/xades")
                        .file(plik)
                        .param("certyfikatId", "cert-2")
                        .param("dokumentReferencja", "wd-test-xades-2"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void podpisanieXades_odrzuconeDlaCertyfikatuUniewaznionego() throws Exception {
        byte[] xml = prostyXml();
        MockMultipartFile plik = new MockMultipartFile("plik", "dok.xml", "application/xml", xml);

        mockMvc.perform(multipart("/api/podpisy/xades")
                        .file(plik)
                        .param("certyfikatId", "cert-3")
                        .param("dokumentReferencja", "wd-test-xades-3"))
                .andExpect(status().isBadRequest());
    }

    /**
     * cert-3 jest uniewazniony - endpoint /api/podpisy/xades go odrzuca (400), wiec testujemy
     * weryfikacje osobno zbudowanego podpisu, obchodzac blokade w PodpisService przez bezposrednie
     * uzycie materialu kryptograficznego wygenerowanego przez DemoPkiInitializer dla cert-3. Dowodzi,
     * ze obliczStatus/znajdzZweryfikowanyCertyfikat sa osiagane przez sciezke XAdES tak samo jak PAdES.
     */
    @Test
    void weryfikacjaXades_podpisCertem3Uniewaznionym_statusCertyfikatUniewazniony() throws Exception {
        CertyfikatEntity cert3 = certyfikatRepository.findById("cert-3").orElseThrow();
        assertThat(cert3.getKeystorePkcs12()).isNotNull();

        byte[] xml = prostyXml();
        byte[] podpisanyXml = podpiszZKeystore(cert3.getKeystorePkcs12(), xml, SignatureLevel.XAdES_BASELINE_T);

        MockMultipartFile plikDoWeryfikacji = new MockMultipartFile("plik", "podpisany.xml", "application/xml", podpisanyXml);
        mockMvc.perform(multipart("/api/podpisy/weryfikacja-xades").file(plikDoWeryfikacji))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("certyfikat_uniewazniony"));
    }

    /**
     * Certyfikat podpisany przez OBCE (nie nasz root/issuing) samopodpisane CA, ale z numerem
     * seryjnym podstawionym na numer_seryjny certyfikatu cert-1 - bez sprawdzenia lancucha
     * leaf -> issuing-CA -> root-CA jako trust anchor, serwis blednie zaakceptowalby ten podpis
     * jako "poprawny" dla cert-1. Mirror weryfikacja_certyfikatPodstawionyZObcegoCa_statusNiepoprawny
     * z PodpisWeryfikacjaTests, dla XAdES.
     */
    @Test
    void weryfikacjaXades_certyfikatPodstawionyZObcegoCa_statusNiepoprawny() throws Exception {
        CertyfikatEntity cert1 = certyfikatRepository.findById("cert-1").orElseThrow();

        KeyPair obceCaKeyPair = demoCertGenerator.generateKeyPair();
        X509Certificate obceCaCert = demoCertGenerator.generateCaCertificate(obceCaKeyPair, "CN=Obce CA XAdES,O=Atakujacy,C=PL");

        KeyPair podstawionyLeafKeyPair = demoCertGenerator.generateKeyPair();
        X509Certificate podstawionyLeafCert = demoCertGenerator.generateLeafCertificate(
                obceCaKeyPair, obceCaCert, podstawionyLeafKeyPair,
                cert1.getNumerSeryjny(), cert1.getPodmiot(),
                LocalDate.now().minusDays(1), LocalDate.now().plusYears(1));

        byte[] podstawionyKeystore = demoCertGenerator.buildPkcs12(
                "podstawiony-xades", podstawionyLeafKeyPair.getPrivate(), KEYSTORE_PASSWORD, podstawionyLeafCert, obceCaCert);

        byte[] xml = prostyXml();
        byte[] podpisanyXml = podpiszZKeystore(podstawionyKeystore, xml, SignatureLevel.XAdES_BASELINE_T);

        MockMultipartFile plikDoWeryfikacji = new MockMultipartFile("plik", "podpisany.xml", "application/xml", podpisanyXml);
        mockMvc.perform(multipart("/api/podpisy/weryfikacja-xades").file(plikDoWeryfikacji))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("niepoprawny"));
    }

    @Test
    void weryfikacjaXades_niepoprawnyPlikXml_400() throws Exception {
        MockMultipartFile smieciowyPlik = new MockMultipartFile("plik", "smieci.xml", "application/xml", "to nie jest xml".getBytes());
        mockMvc.perform(multipart("/api/podpisy/weryfikacja-xades").file(smieciowyPlik))
                .andExpect(status().isBadRequest());
    }

    /**
     * Zgodnosc wsteczna: podpis BASELINE-B (bez znacznika czasu) musi nadal weryfikowac sie
     * poprawnie - znacznikCzasuTsa spada na wartosc "brak". Mirror odpowiadajacego testu PAdES.
     */
    @Test
    void weryfikacjaXades_podpisBaselineBBezTsa_znacznikCzasuBrak() throws Exception {
        CertyfikatEntity cert1 = certyfikatRepository.findById("cert-1").orElseThrow();
        byte[] xml = prostyXml();
        byte[] podpisanyXml = podpiszZKeystore(cert1.getKeystorePkcs12(), xml, SignatureLevel.XAdES_BASELINE_B);

        MockMultipartFile plikDoWeryfikacji = new MockMultipartFile("plik", "podpisany.xml", "application/xml", podpisanyXml);
        mockMvc.perform(multipart("/api/podpisy/weryfikacja-xades").file(plikDoWeryfikacji))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("poprawny"))
                .andExpect(jsonPath("$.znacznikCzasuTsa").value("brak"));
    }

    private byte[] podpiszZKeystore(byte[] keystorePkcs12, byte[] xml, SignatureLevel signatureLevel) throws Exception {
        try (Pkcs12SignatureToken token = new Pkcs12SignatureToken(
                keystorePkcs12, new KeyStore.PasswordProtection(KEYSTORE_PASSWORD))) {
            List<DSSPrivateKeyEntry> keys = token.getKeys();
            DSSPrivateKeyEntry privateKeyEntry = keys.get(0);
            var certificateVerifier = PodpisService.buildOfflineCertificateVerifier();
            XAdESService service = new XAdESService(certificateVerifier);
            if (signatureLevel == SignatureLevel.XAdES_BASELINE_T) {
                DemoTsaEntity demoTsa = demoTsaRepository.findById("demo-tsa-1").orElseThrow();
                KeyEntityTSPSource tspSource = new KeyEntityTSPSource(
                        demoTsa.getKeystorePkcs12(), "PKCS12", KEYSTORE_PASSWORD, "demo-tsa", KEYSTORE_PASSWORD);
                tspSource.setTsaPolicy("1.2.3.4.5.6");
                service.setTspSource(tspSource);
            }
            XAdESSignatureParameters parameters = new XAdESSignatureParameters();
            parameters.setSignatureLevel(signatureLevel);
            parameters.setSignaturePackaging(SignaturePackaging.ENVELOPED);
            parameters.setDigestAlgorithm(DigestAlgorithm.SHA256);
            parameters.setSigningCertificate(privateKeyEntry.getCertificate());
            parameters.setCertificateChain(privateKeyEntry.getCertificateChain());

            DSSDocument toSignDocument = new InMemoryDocument(xml, "dokument.xml");
            ToBeSigned dataToSign = service.getDataToSign(toSignDocument, parameters);
            SignatureValue signatureValue = token.sign(dataToSign, parameters.getDigestAlgorithm(), privateKeyEntry);
            DSSDocument signedDocument = service.signDocument(toSignDocument, parameters, signatureValue);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            signedDocument.writeTo(out);
            return out.toByteArray();
        }
    }
}
