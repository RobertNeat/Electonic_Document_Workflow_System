package robert_neat.sign_service.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.sign_service.entity.CertyfikatEntity;
import robert_neat.sign_service.entity.DemoCaEntity;
import robert_neat.sign_service.entity.DemoIssuingCaEntity;
import robert_neat.sign_service.entity.DemoTsaEntity;
import robert_neat.sign_service.repository.CertyfikatRepository;
import robert_neat.sign_service.repository.DemoCaRepository;
import robert_neat.sign_service.repository.DemoIssuingCaRepository;
import robert_neat.sign_service.repository.DemoTsaRepository;
import robert_neat.sign_service.service.DemoCertGenerator;

import java.time.LocalDate;

import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Inicjalizuje root-CA (jesli brak), issuing-CA (jesli brak, podpisany przez root), demo-TSA
 * (jesli brak, podpisany przez issuing-CA, mock RFC 3161 dla PAdES BASELINE-T) i generuje
 * certyfikaty leaf (podpisane przez issuing-CA) dla wierszy `certyfikat` bez materialu
 * kryptograficznego (seed demo: cert-1/2/3). Idempotentny - uruchomienie przy kolejnym starcie z
 * tymi samymi danymi w bazie nie duplikuje `demo_ca`/`demo_issuing_ca`/`demo_tsa` ani nie
 * nadpisuje juz wygenerowanych certyfikatow (sprawdzenie "czy puste" przed generowaniem).
 */
@Configuration
@EnableConfigurationProperties(PkiProperties.class)
public class DemoPkiInitializer implements ApplicationRunner {

    private static final String DEMO_CA_ID = "demo-ca-1";
    private static final String DEMO_CA_SUBJECT_DN = "CN=Demo CA Root,O=Urzad,C=PL";
    private static final String DEMO_CA_ALIAS = "demo-ca";
    private static final String DEMO_ISSUING_CA_ID = "demo-issuing-ca-1";
    private static final String DEMO_ISSUING_CA_SUBJECT_DN = "CN=Demo CA Issuing,O=Urzad,C=PL";
    private static final String DEMO_ISSUING_CA_ALIAS = "demo-issuing-ca";
    // Odrebny od BigInteger.ONE (numer seryjny roota) i od DEMO_TSA_SERIAL_HEX - RFC 5280 4.1.2.2
    // zakazuje dwoch certyfikatow tego samego wydawcy z identycznym numerem seryjnym; root wydaje
    // tylko ten jeden certyfikat (issuing-CA), ale numer musi byc jednoznaczny w calej hierarchii.
    private static final String DEMO_ISSUING_CA_SERIAL_HEX = "7E5A0002";
    private static final String DEMO_TSA_ID = "demo-tsa-1";
    private static final String DEMO_TSA_SUBJECT_DN = "CN=Demo TSA,O=Urzad,C=PL";
    private static final String DEMO_TSA_ALIAS = "demo-tsa";
    // Nie "01" - demo-TSA jest wydany z tym samym BigInteger.ONE jako wlasny numer seryjny root-CA;
    // RFC 5280 4.1.2.2 zakazuje dwoch certyfikatow tego samego wydawcy z identycznym numerem
    // seryjnym; demo-TSA jest teraz wydawany przez issuing-CA, nie przez root, wiec ten numer juz
    // nie koliduje z numerem seryjnym roota - zachowany jednak jako odrebna, jednoznaczna wartosc.
    private static final String DEMO_TSA_SERIAL_HEX = "7E5A0001";

    private final DemoCaRepository demoCaRepository;
    private final DemoIssuingCaRepository demoIssuingCaRepository;
    private final DemoTsaRepository demoTsaRepository;
    private final CertyfikatRepository certyfikatRepository;
    private final DemoCertGenerator certGenerator;
    private final PkiProperties pkiProperties;

    public DemoPkiInitializer(DemoCaRepository demoCaRepository, DemoIssuingCaRepository demoIssuingCaRepository,
                               DemoTsaRepository demoTsaRepository, CertyfikatRepository certyfikatRepository,
                               DemoCertGenerator certGenerator, PkiProperties pkiProperties) {
        this.demoCaRepository = demoCaRepository;
        this.demoIssuingCaRepository = demoIssuingCaRepository;
        this.demoTsaRepository = demoTsaRepository;
        this.certyfikatRepository = certyfikatRepository;
        this.certGenerator = certGenerator;
        this.pkiProperties = pkiProperties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        char[] password = pkiProperties.getKeystorePassword().toCharArray();

        DemoCaEntity demoCa = demoCaRepository.findById(DEMO_CA_ID).orElse(null);
        KeyPair rootKeyPair;
        X509Certificate rootCertificate;
        if (demoCa == null) {
            rootKeyPair = certGenerator.generateKeyPair();
            rootCertificate = certGenerator.generateCaCertificate(rootKeyPair, DEMO_CA_SUBJECT_DN);
            byte[] rootDer = rootCertificate.getEncoded();
            byte[] rootPkcs12 = certGenerator.buildPkcs12(DEMO_CA_ALIAS, rootKeyPair.getPrivate(), password, rootCertificate);

            demoCa = new DemoCaEntity();
            demoCa.setId(DEMO_CA_ID);
            demoCa.setCertyfikatDer(rootDer);
            demoCa.setKeystorePkcs12(rootPkcs12);
            demoCa.setDataUtworzenia(OffsetDateTime.now());
            demoCaRepository.save(demoCa);
        } else {
            rootCertificate = loadCertificate(demoCa.getCertyfikatDer());
            rootKeyPair = loadKeyPairFromPkcs12(demoCa.getKeystorePkcs12(), DEMO_CA_ALIAS, password, rootCertificate);
        }

        DemoIssuingCaEntity demoIssuingCa = demoIssuingCaRepository.findById(DEMO_ISSUING_CA_ID).orElse(null);
        KeyPair issuingKeyPair;
        X509Certificate issuingCertificate;
        if (demoIssuingCa == null) {
            issuingKeyPair = certGenerator.generateKeyPair();
            issuingCertificate = certGenerator.generateIssuingCaCertificate(
                    rootKeyPair, rootCertificate, issuingKeyPair, DEMO_ISSUING_CA_SERIAL_HEX, DEMO_ISSUING_CA_SUBJECT_DN,
                    LocalDate.of(2020, 1, 1), LocalDate.of(2045, 1, 1));
            byte[] issuingDer = issuingCertificate.getEncoded();
            byte[] issuingPkcs12 = certGenerator.buildPkcs12(
                    DEMO_ISSUING_CA_ALIAS, issuingKeyPair.getPrivate(), password, issuingCertificate, rootCertificate);

            demoIssuingCa = new DemoIssuingCaEntity();
            demoIssuingCa.setId(DEMO_ISSUING_CA_ID);
            demoIssuingCa.setCertyfikatDer(issuingDer);
            demoIssuingCa.setKeystorePkcs12(issuingPkcs12);
            demoIssuingCa.setDataUtworzenia(OffsetDateTime.now());
            demoIssuingCaRepository.save(demoIssuingCa);
        } else {
            issuingCertificate = loadCertificate(demoIssuingCa.getCertyfikatDer());
            issuingKeyPair = loadKeyPairFromPkcs12(demoIssuingCa.getKeystorePkcs12(), DEMO_ISSUING_CA_ALIAS, password, issuingCertificate);
        }

        if (demoTsaRepository.findById(DEMO_TSA_ID).isEmpty()) {
            KeyPair tsaKeyPair = certGenerator.generateKeyPair();
            X509Certificate tsaCertificate = certGenerator.generateTsaCertificate(
                    issuingKeyPair, issuingCertificate, tsaKeyPair, DEMO_TSA_SERIAL_HEX, DEMO_TSA_SUBJECT_DN,
                    LocalDate.of(2020, 1, 1), LocalDate.of(2045, 1, 1));
            byte[] tsaDer = tsaCertificate.getEncoded();
            byte[] tsaPkcs12 = certGenerator.buildPkcs12(
                    DEMO_TSA_ALIAS, tsaKeyPair.getPrivate(), password, tsaCertificate, issuingCertificate, rootCertificate);

            DemoTsaEntity demoTsa = new DemoTsaEntity();
            demoTsa.setId(DEMO_TSA_ID);
            demoTsa.setCertyfikatDer(tsaDer);
            demoTsa.setKeystorePkcs12(tsaPkcs12);
            demoTsa.setDataUtworzenia(OffsetDateTime.now());
            demoTsaRepository.save(demoTsa);
        }

        List<CertyfikatEntity> bezMaterialu = certyfikatRepository.findAll().stream()
                .filter(c -> c.getCertyfikatDer() == null || c.getKeystorePkcs12() == null)
                .toList();

        for (CertyfikatEntity certyfikat : bezMaterialu) {
            KeyPair leafKeyPair = certGenerator.generateKeyPair();
            X509Certificate leafCertificate = certGenerator.generateLeafCertificate(
                    issuingKeyPair, issuingCertificate, leafKeyPair,
                    certyfikat.getNumerSeryjny(), certyfikat.getPodmiot(),
                    certyfikat.getDataWydania(), certyfikat.getDataWygasniecia());
            byte[] leafDer = leafCertificate.getEncoded();
            byte[] leafPkcs12 = certGenerator.buildPkcs12(
                    certyfikat.getId(), leafKeyPair.getPrivate(), password, leafCertificate, issuingCertificate, rootCertificate);

            certyfikat.setCertyfikatDer(leafDer);
            certyfikat.setKeystorePkcs12(leafPkcs12);
            certyfikatRepository.save(certyfikat);
        }
    }

    private static X509Certificate loadCertificate(byte[] der) throws Exception {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) factory.generateCertificate(new java.io.ByteArrayInputStream(der));
    }

    private static KeyPair loadKeyPairFromPkcs12(byte[] pkcs12, String alias, char[] password, X509Certificate certificate) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(new java.io.ByteArrayInputStream(pkcs12), password);
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, password);
        return new KeyPair(certificate.getPublicKey(), privateKey);
    }
}
