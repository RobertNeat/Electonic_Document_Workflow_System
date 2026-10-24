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
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.ocsp.BasicOCSPRespBuilder;
import org.bouncycastle.cert.ocsp.CertificateID;
import org.bouncycastle.cert.ocsp.OCSPReq;
import org.bouncycastle.cert.ocsp.OCSPRespBuilder;
import org.bouncycastle.cert.ocsp.Req;
import org.bouncycastle.cert.ocsp.RevokedStatus;
import org.bouncycastle.cert.ocsp.jcajce.JcaBasicOCSPRespBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import robert_neat.sign_service.config.PkiProperties;
import robert_neat.sign_service.entity.CertyfikatEntity;
import robert_neat.sign_service.entity.DemoIssuingCaEntity;
import robert_neat.sign_service.repository.CertyfikatRepository;
import robert_neat.sign_service.repository.DemoIssuingCaRepository;
import robert_neat.sign_service.repository.UniewaznienieRepository;
import robert_neat.sign_service.revocation.CrlResponderService;
import robert_neat.sign_service.service.DemoCertGenerator;
import robert_neat.sign_service.service.PodpisService;
import robert_neat.sign_service.service.WeryfikacjaService;

import java.io.ByteArrayOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

/**
 * Testy DOWODZACE, ze status unieważnienia faktycznie pochodzi z sieciowego wywolania OCSP/CRL
 * (nie z lokalnej tabeli `uniewaznienie`, ktora dla cert-1 jest PUSTA) - zob. zadanie plan.md.
 * Rejestruje WLASNE, precyzyjne (nie-delegujace) oczekiwania MockRestServiceServer per test,
 * zamiast domyslnych stubow RevocationStubs.
 */
@Import({TestcontainersConfiguration.class, RevocationMockTransportConfiguration.class})
@SpringBootTest
class WeryfikacjaServiceRewokacjaSiecowaTests {

    private static final char[] KEYSTORE_PASSWORD = "demo-keystore-password".toCharArray();

    @Autowired
    private WeryfikacjaService weryfikacjaService;

    @Autowired
    private CertyfikatRepository certyfikatRepository;

    @Autowired
    private UniewaznienieRepository uniewaznienieRepository;

    @Autowired
    private DemoIssuingCaRepository demoIssuingCaRepository;

    @Autowired
    private PkiProperties pkiProperties;

    @Autowired
    private RevocationMockTransportConfiguration.MockRevocationTransport mockRevocationTransport;

    @Autowired
    private CrlResponderService crlResponderService;

    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        server = mockRevocationTransport.server();
        server.reset();
    }

    private static byte[] prostyPdf() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.save(out);
        }
        return out.toByteArray();
    }

    /**
     * Dowod load-bearing nr 1: cert-1 NIE MA zadnego wiersza w `uniewaznienie`, ale OCSP respondera
     * (prawidlowo podpisana odpowiedz issuing-CA) mowi "revoked" - wynik koncowy MUSI byc
     * certyfikat_uniewazniony. Gdyby serwis nadal czytal lokalna tabele `uniewaznienie` (stary
     * kod), wynik bylby "poprawny" - ten test odrozniłby te dwie sciezki.
     */
    @Test
    void certyfikatBezWpisuUniewaznienia_OcspMowiRevoked_statusCertyfikatUniewazniony() throws Exception {
        CertyfikatEntity cert1 = certyfikatRepository.findById("cert-1").orElseThrow();
        assertThat(uniewaznienieRepository.findByCertyfikat_Id("cert-1")).isEmpty();

        X509Certificate issuingCaCert = wczytajIssuingCaCert();
        KeyPair issuingCaKeyPair = wczytajIssuingCaKeyPair(issuingCaCert);

        server.expect(ExpectedCount.once(), requestTo(pkiProperties.getPublicBaseUrl() + "/api/ocsp"))
                .andExpect(method(POST))
                .andRespond(request -> odpowiedzOcspRevoked(request, issuingCaCert, issuingCaKeyPair));
        // Zadnego oczekiwania CRL - jesli OCSP nie dalby wiarygodnej odpowiedzi, kod spadnie na
        // CRL i MockRestServiceServer zglosi AssertionError (niezarejestrowane zadanie) - to jest
        // zamierzony tripwire potwierdzajacy, ze OCSP wystarczyl.

        byte[] pdf = prostyPdf();
        byte[] podpisanyPdf = podpiszZKeystore(cert1.getKeystorePkcs12(), pdf);

        var wynik = weryfikacjaService.weryfikujPades(podpisanyPdf);
        assertThat(wynik.status()).isEqualTo("certyfikat_uniewazniony");
        server.verify();
    }

    /**
     * Dowod load-bearing nr 2: odpowiedz OCSP podpisana FALSZYWYM kluczem (nie issuing-CA) mowiaca
     * "good" MUSI zostac odrzucona (podpis sie nie zweryfikuje) - serwis spada na CRL, ktory tu
     * zwraca 500 (niedostepny) - wynik fail-closed: niepoprawny. server.verify() potwierdza, ze
     * OBA kanaly zostaly faktycznie odpytane (nie np. odrzucone wczesniej na etapie lancucha).
     */
    @Test
    void ocspOdpowiedzSfalszowanaZlymKluczem_iCrlNiedostepny_statusNiepoprawny() throws Exception {
        CertyfikatEntity cert1 = certyfikatRepository.findById("cert-1").orElseThrow();

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair falszywyKeyPair = generator.generateKeyPair();

        server.expect(ExpectedCount.once(), requestTo(pkiProperties.getPublicBaseUrl() + "/api/ocsp"))
                .andExpect(method(POST))
                .andRespond(request -> odpowiedzOcspGoodPodpisanaFalszywymKluczem(request, falszywyKeyPair));
        server.expect(ExpectedCount.once(), requestTo(pkiProperties.getPublicBaseUrl() + "/api/crl/issuing-ca.crl"))
                .andExpect(method(GET))
                .andRespond(request -> new MockClientHttpResponse(new byte[0], HttpStatus.INTERNAL_SERVER_ERROR));

        byte[] pdf = prostyPdf();
        byte[] podpisanyPdf = podpiszZKeystore(cert1.getKeystorePkcs12(), pdf);

        var wynik = weryfikacjaService.weryfikujPades(podpisanyPdf);
        assertThat(wynik.status()).isEqualTo("niepoprawny");
        server.verify();
    }

    /**
     * Dowod load-bearing nr 3: sciezka fallback CRL faktycznie dziala end-to-end (nie tylko jako
     * logika serwerowa testowana osobno w OcspCrlResponderTests) - OCSP niedostepny (500), CRL
     * delegowany do PRAWDZIWEGO CrlResponderService (ta sama baza testowa) zwraca cert-3 jako
     * unieważniony - CrlClient musi: pobrac CRL, zweryfikowac jego podpis kluczem publicznym
     * issuing-CA, znalezc numer seryjny cert-3 na liscie.
     */
    @Test
    void ocspNiedostepny_crlRespondeDelegowanyDoPrawdziwejLogiki_cert3_statusCertyfikatUniewazniony() throws Exception {
        CertyfikatEntity cert3 = certyfikatRepository.findById("cert-3").orElseThrow();

        server.expect(ExpectedCount.once(), requestTo(pkiProperties.getPublicBaseUrl() + "/api/ocsp"))
                .andExpect(method(POST))
                .andRespond(request -> new MockClientHttpResponse(new byte[0], HttpStatus.INTERNAL_SERVER_ERROR));
        server.expect(ExpectedCount.once(), requestTo(pkiProperties.getPublicBaseUrl() + "/api/crl/issuing-ca.crl"))
                .andExpect(method(GET))
                .andRespond(request -> odpowiedzCrlPrawdziwa());

        byte[] pdf = prostyPdf();
        byte[] podpisanyPdf = podpiszZKeystore(cert3.getKeystorePkcs12(), pdf);

        var wynik = weryfikacjaService.weryfikujPades(podpisanyPdf);
        assertThat(wynik.status()).isEqualTo("certyfikat_uniewazniony");
        server.verify();
    }

    /**
     * Kontrola pozytywna dla dowodu nr 3: ten sam fallback CRL (prawdziwa logika, zaufany podpis),
     * ale dla cert-1 (brak wpisu uniewaznienie) - CRL "nie na liscie" musi dac wynik "poprawny",
     * nie fail-closed. Wyklucza falszywie-pozytywny wynik powyzszego testu (np. gdyby kazdy wynik
     * CRL dawal "uniewazniony" niezaleznie od zawartosci).
     */
    @Test
    void ocspNiedostepny_crlRespondeDelegowanyDoPrawdziwejLogiki_cert1_statusPoprawny() throws Exception {
        CertyfikatEntity cert1 = certyfikatRepository.findById("cert-1").orElseThrow();

        server.expect(ExpectedCount.once(), requestTo(pkiProperties.getPublicBaseUrl() + "/api/ocsp"))
                .andExpect(method(POST))
                .andRespond(request -> new MockClientHttpResponse(new byte[0], HttpStatus.INTERNAL_SERVER_ERROR));
        server.expect(ExpectedCount.once(), requestTo(pkiProperties.getPublicBaseUrl() + "/api/crl/issuing-ca.crl"))
                .andExpect(method(GET))
                .andRespond(request -> odpowiedzCrlPrawdziwa());

        byte[] pdf = prostyPdf();
        byte[] podpisanyPdf = podpiszZKeystore(cert1.getKeystorePkcs12(), pdf);

        var wynik = weryfikacjaService.weryfikujPades(podpisanyPdf);
        assertThat(wynik.status()).isEqualTo("poprawny");
        server.verify();
    }

    private MockClientHttpResponse odpowiedzCrlPrawdziwa() {
        byte[] crlBytes = crlResponderService.buildCrl();
        MockClientHttpResponse response = new MockClientHttpResponse(crlBytes, HttpStatus.OK);
        response.getHeaders().add("Content-Type", "application/pkix-crl");
        return response;
    }

    private MockClientHttpResponse odpowiedzOcspRevoked(org.springframework.http.client.ClientHttpRequest request,
                                                          X509Certificate issuingCaCert, KeyPair issuingCaKeyPair) {
        try {
            byte[] requestBody = ((MockClientHttpRequest) request).getBodyAsBytes();
            OCSPReq ocspReq = new OCSPReq(requestBody);
            Req[] requestList = ocspReq.getRequestList();
            CertificateID certificateId = requestList[0].getCertID();

            X509CertificateHolder issuingCaHolder = new JcaX509CertificateHolder(issuingCaCert);
            var digestCalculator = new JcaDigestCalculatorProviderBuilder().build().get(CertificateID.HASH_SHA1);
            BasicOCSPRespBuilder builder = new JcaBasicOCSPRespBuilder(issuingCaCert.getPublicKey(), digestCalculator);

            Date now = new Date();
            builder.addResponse(certificateId, new RevokedStatus(now, 0), now, new Date(now.getTime() + 60_000), null);

            ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(issuingCaKeyPair.getPrivate());
            var basicResp = builder.build(signer, new X509CertificateHolder[]{issuingCaHolder}, now);
            byte[] responseBytes = new OCSPRespBuilder().build(OCSPRespBuilder.SUCCESSFUL, basicResp).getEncoded();

            MockClientHttpResponse response = new MockClientHttpResponse(responseBytes, HttpStatus.OK);
            response.getHeaders().add("Content-Type", "application/ocsp-response");
            return response;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private MockClientHttpResponse odpowiedzOcspGoodPodpisanaFalszywymKluczem(
            org.springframework.http.client.ClientHttpRequest request, KeyPair falszywyKeyPair) {
        try {
            byte[] requestBody = ((MockClientHttpRequest) request).getBodyAsBytes();
            OCSPReq ocspReq = new OCSPReq(requestBody);
            Req[] requestList = ocspReq.getRequestList();
            CertificateID certificateId = requestList[0].getCertID();

            // Falszywy "issuer" dla celu podpisania odpowiedzi - certyfikat nigdy nie jest wyslany
            // do klienta jako zaufany, liczy sie tylko to, ze podpis odpowiedzi NIE pochodzi od
            // prawdziwego issuing-CA, wiec OcspClient.sprawdz() musi go odrzucic jako
            // nieautentyczny.
            X509Certificate falszywyCert = new DemoCertGenerator()
                    .generateCaCertificate(falszywyKeyPair, "CN=Falszywy Responder,O=Atakujacy,C=PL");
            X509CertificateHolder falszywyHolder = new JcaX509CertificateHolder(falszywyCert);
            var digestCalculator = new JcaDigestCalculatorProviderBuilder().build().get(CertificateID.HASH_SHA1);
            BasicOCSPRespBuilder builder = new JcaBasicOCSPRespBuilder(falszywyCert.getPublicKey(), digestCalculator);

            Date now = new Date();
            builder.addResponse(certificateId, null, now, new Date(now.getTime() + 60_000), null);

            ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(falszywyKeyPair.getPrivate());
            var basicResp = builder.build(signer, new X509CertificateHolder[]{falszywyHolder}, now);
            byte[] responseBytes = new OCSPRespBuilder().build(OCSPRespBuilder.SUCCESSFUL, basicResp).getEncoded();

            MockClientHttpResponse response = new MockClientHttpResponse(responseBytes, HttpStatus.OK);
            response.getHeaders().add("Content-Type", "application/ocsp-response");
            return response;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private X509Certificate wczytajIssuingCaCert() throws Exception {
        DemoIssuingCaEntity entity = demoIssuingCaRepository.findById("demo-issuing-ca-1").orElseThrow();
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) factory.generateCertificate(new java.io.ByteArrayInputStream(entity.getCertyfikatDer()));
    }

    private KeyPair wczytajIssuingCaKeyPair(X509Certificate issuingCaCert) throws Exception {
        DemoIssuingCaEntity entity = demoIssuingCaRepository.findById("demo-issuing-ca-1").orElseThrow();
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(new java.io.ByteArrayInputStream(entity.getKeystorePkcs12()), KEYSTORE_PASSWORD);
        var privateKey = (java.security.PrivateKey) keyStore.getKey("demo-issuing-ca", KEYSTORE_PASSWORD);
        return new KeyPair(issuingCaCert.getPublicKey(), privateKey);
    }

    private static byte[] podpiszZKeystore(byte[] keystorePkcs12, byte[] pdf) throws Exception {
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
}
