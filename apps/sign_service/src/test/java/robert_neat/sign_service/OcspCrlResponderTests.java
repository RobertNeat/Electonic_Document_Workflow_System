package robert_neat.sign_service;

import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.ocsp.BasicOCSPResp;
import org.bouncycastle.cert.ocsp.CertificateID;
import org.bouncycastle.cert.ocsp.OCSPReq;
import org.bouncycastle.cert.ocsp.OCSPReqBuilder;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.bouncycastle.cert.ocsp.OCSPRespBuilder;
import org.bouncycastle.cert.ocsp.RevokedStatus;
import org.bouncycastle.cert.ocsp.SingleResp;
import org.bouncycastle.cert.ocsp.UnknownStatus;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import robert_neat.sign_service.entity.CertyfikatEntity;
import robert_neat.sign_service.entity.DemoIssuingCaEntity;
import robert_neat.sign_service.repository.CertyfikatRepository;
import robert_neat.sign_service.repository.DemoIssuingCaRepository;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy endpointow OCSP (/api/ocsp) i CRL (/api/crl/issuing-ca.crl) poprzez MockMvc - weryfikuja
 * sama warstwe HTTP (content-type, binding byte[], status 200 nawet dla MALFORMED_REQUEST) ktorej
 * RevocationStubs (wolajace responder bezposrednio) nie dotyka. Seed demo: cert-1 aktywny (brak
 * wpisu uniewaznienie), cert-3 uniewazniony (zob. 100-seed.xml).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class OcspCrlResponderTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CertyfikatRepository certyfikatRepository;

    @Autowired
    private DemoIssuingCaRepository demoIssuingCaRepository;

    @Test
    void ocsp_certAktywny_statusGoodIPodpisPoprawny() throws Exception {
        X509Certificate issuingCaCert = wczytajIssuingCaCert();
        CertyfikatEntity cert1 = certyfikatRepository.findById("cert-1").orElseThrow();

        OCSPReq req = zbudujZadanie(issuingCaCert, cert1.getNumerSeryjny());
        byte[] responseBytes = wyslijOcsp(req.getEncoded());

        BasicOCSPResp basicResp = rozpakuj(responseBytes);
        assertThat(basicResp.isSignatureValid(
                new JcaContentVerifierProviderBuilder().build(issuingCaCert.getPublicKey()))).isTrue();
        SingleResp singleResp = basicResp.getResponses()[0];
        assertThat(singleResp.getCertStatus()).isNull();
    }

    @Test
    void ocsp_certUniewazniony_statusRevoked() throws Exception {
        X509Certificate issuingCaCert = wczytajIssuingCaCert();
        CertyfikatEntity cert3 = certyfikatRepository.findById("cert-3").orElseThrow();

        OCSPReq req = zbudujZadanie(issuingCaCert, cert3.getNumerSeryjny());
        byte[] responseBytes = wyslijOcsp(req.getEncoded());

        BasicOCSPResp basicResp = rozpakuj(responseBytes);
        SingleResp singleResp = basicResp.getResponses()[0];
        assertThat(singleResp.getCertStatus()).isInstanceOf(RevokedStatus.class);
        RevokedStatus revokedStatus = (RevokedStatus) singleResp.getCertStatus();
        // Porownanie z dokladnoscia do sekund - GeneralizedTime (OCSP) obcina precyzje ponizej
        // sekundy, a kolumna data_uniewaznienia w Postgres jest mikrosekundowa.
        assertThat(revokedStatus.getRevocationTime().toInstant().getEpochSecond())
                .isEqualTo(Instant.parse("2026-09-20T00:00:00Z").getEpochSecond());
    }

    @Test
    void ocsp_numerSeryjnyNieznany_statusUnknown() throws Exception {
        X509Certificate issuingCaCert = wczytajIssuingCaCert();

        OCSPReq req = zbudujZadanie(issuingCaCert, "DEADBEEF00112233");
        byte[] responseBytes = wyslijOcsp(req.getEncoded());

        BasicOCSPResp basicResp = rozpakuj(responseBytes);
        assertThat(basicResp.getResponses()[0].getCertStatus()).isInstanceOf(UnknownStatus.class);
    }

    @Test
    void ocsp_certificateIdZObcegoIssuera_statusUnknown() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair obcyKeyPair = generator.generateKeyPair();
        X509Certificate obcyIssuerCert = new robert_neat.sign_service.service.DemoCertGenerator()
                .generateCaCertificate(obcyKeyPair, "CN=Obcy Issuer,O=Atakujacy,C=PL");

        CertyfikatEntity cert1 = certyfikatRepository.findById("cert-1").orElseThrow();
        OCSPReq req = zbudujZadanie(obcyIssuerCert, cert1.getNumerSeryjny());
        byte[] responseBytes = wyslijOcsp(req.getEncoded());

        BasicOCSPResp basicResp = rozpakuj(responseBytes);
        assertThat(basicResp.getResponses()[0].getCertStatus()).isInstanceOf(UnknownStatus.class);
    }

    @Test
    void ocsp_zadanieZNonce_echoNonceWOdpowiedzi() throws Exception {
        X509Certificate issuingCaCert = wczytajIssuingCaCert();
        CertyfikatEntity cert1 = certyfikatRepository.findById("cert-1").orElseThrow();

        var digestCalculator = new JcaDigestCalculatorProviderBuilder().build().get(CertificateID.HASH_SHA1);
        X509CertificateHolder issuingCaHolder = new JcaX509CertificateHolder(issuingCaCert);
        CertificateID certificateId = new CertificateID(digestCalculator, issuingCaHolder,
                new BigInteger(cert1.getNumerSeryjny(), 16));

        byte[] nonceBytes = "nonce-test-12345".getBytes();
        Extensions reqExtensions = new Extensions(new Extension(
                org.bouncycastle.asn1.ocsp.OCSPObjectIdentifiers.id_pkix_ocsp_nonce, false,
                new DEROctetString(nonceBytes)));

        OCSPReqBuilder reqBuilder = new OCSPReqBuilder();
        reqBuilder.addRequest(certificateId);
        reqBuilder.setRequestExtensions(reqExtensions);
        OCSPReq req = reqBuilder.build();

        byte[] responseBytes = wyslijOcsp(req.getEncoded());
        BasicOCSPResp basicResp = rozpakuj(responseBytes);
        Extension echoedNonce = basicResp.getExtension(org.bouncycastle.asn1.ocsp.OCSPObjectIdentifiers.id_pkix_ocsp_nonce);
        assertThat(echoedNonce).isNotNull();
        assertThat(echoedNonce.getExtnValue().getOctets()).isEqualTo(new DEROctetString(nonceBytes).getOctets());
    }

    @Test
    void ocsp_smieciowyBody_malformedRequestStatusHttp200() throws Exception {
        byte[] responseBytes = wyslijOcsp("to nie jest OCSPReq".getBytes());

        OCSPResp ocspResp = new OCSPResp(responseBytes);
        assertThat(ocspResp.getStatus()).isEqualTo(OCSPRespBuilder.MALFORMED_REQUEST);
    }

    @Test
    void ocsp_pustyBody_malformedRequestStatusHttp200() throws Exception {
        byte[] responseBytes = wyslijOcsp(new byte[0]);

        OCSPResp ocspResp = new OCSPResp(responseBytes);
        assertThat(ocspResp.getStatus()).isEqualTo(OCSPRespBuilder.MALFORMED_REQUEST);
    }

    @Test
    void crl_pobranie_podpisPoprawnyIZawieraCert3ANieCert1() throws Exception {
        X509Certificate issuingCaCert = wczytajIssuingCaCert();
        CertyfikatEntity cert1 = certyfikatRepository.findById("cert-1").orElseThrow();
        CertyfikatEntity cert3 = certyfikatRepository.findById("cert-3").orElseThrow();

        byte[] crlBytes = mockMvc.perform(get("/api/crl/issuing-ca.crl"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        X509CRL crl = (X509CRL) factory.generateCRL(new java.io.ByteArrayInputStream(crlBytes));
        crl.verify(issuingCaCert.getPublicKey());

        assertThat(crl.getRevokedCertificate(new BigInteger(cert3.getNumerSeryjny(), 16))).isNotNull();
        assertThat(crl.getRevokedCertificate(new BigInteger(cert1.getNumerSeryjny(), 16))).isNull();
        assertThat(crl.getExtensionValue(Extension.cRLNumber.getId())).isNotNull();
        assertThat(crl.getNextUpdate()).isAfter(new java.util.Date());
    }

    private X509Certificate wczytajIssuingCaCert() throws Exception {
        DemoIssuingCaEntity entity = demoIssuingCaRepository.findById("demo-issuing-ca-1").orElseThrow();
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) factory.generateCertificate(new java.io.ByteArrayInputStream(entity.getCertyfikatDer()));
    }

    private static OCSPReq zbudujZadanie(X509Certificate issuerCert, String serialHex) throws Exception {
        var digestCalculator = new JcaDigestCalculatorProviderBuilder().build().get(CertificateID.HASH_SHA1);
        X509CertificateHolder issuerHolder = new JcaX509CertificateHolder(issuerCert);
        CertificateID certificateId = new CertificateID(digestCalculator, issuerHolder, new BigInteger(serialHex, 16));
        OCSPReqBuilder reqBuilder = new OCSPReqBuilder();
        reqBuilder.addRequest(certificateId);
        return reqBuilder.build();
    }

    private byte[] wyslijOcsp(byte[] requestDer) throws Exception {
        return mockMvc.perform(post("/api/ocsp")
                        .contentType("application/ocsp-request")
                        .content(requestDer))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
    }

    private static BasicOCSPResp rozpakuj(byte[] responseBytes) throws Exception {
        OCSPResp ocspResp = new OCSPResp(responseBytes);
        assertThat(ocspResp.getStatus()).isEqualTo(OCSPRespBuilder.SUCCESSFUL);
        return (BasicOCSPResp) ocspResp.getResponseObject();
    }
}
