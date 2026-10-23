package robert_neat.sign_service.service;

import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.pades.validation.PDFDocumentValidator;
import eu.europa.esig.dss.spi.signature.AdvancedSignature;
import eu.europa.esig.dss.spi.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.spi.x509.tsp.KeyEntityTSPSource;
import eu.europa.esig.dss.spi.x509.tsp.TSPSource;
import eu.europa.esig.dss.spi.x509.tsp.TimestampToken;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.Pkcs12SignatureToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.sign_service.config.PkiProperties;
import robert_neat.sign_service.entity.CertyfikatEntity;
import robert_neat.sign_service.entity.DemoTsaEntity;
import robert_neat.sign_service.entity.WystawionyPodpisEntity;
import robert_neat.sign_service.exception.BadRequestException;
import robert_neat.sign_service.exception.NotFoundException;
import robert_neat.sign_service.repository.CertyfikatRepository;
import robert_neat.sign_service.repository.DemoTsaRepository;
import robert_neat.sign_service.repository.WystawionyPodpisRepository;

import java.io.ByteArrayOutputStream;
import java.security.KeyStore;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Skladanie podpisu PAdES BASELINE-T (znacznik czasu z mock-TSA zgodnego z RFC 3161 - zob.
 * DemoCertGenerator/DemoPkiInitializer). `dms_service`/dokument nie jest pobierany przez ten
 * serwis - zadanie dostarcza bajty PDF do podpisania bezposrednio.
 */
@Service
public class PodpisService {

    private static final String DEMO_TSA_ID = "demo-tsa-1";
    private static final String DEMO_TSA_ALIAS = "demo-tsa";
    // Arbitralny OID polityki demo-TSA (nieprzypisany) - KeyEntityTSPSource wymaga wartosci.
    private static final String DEMO_TSA_POLICY_OID = "1.2.3.4.5.6";

    private final CertyfikatRepository certyfikatRepository;
    private final WystawionyPodpisRepository wystawionyPodpisRepository;
    private final DemoTsaRepository demoTsaRepository;
    private final PkiProperties pkiProperties;

    public PodpisService(CertyfikatRepository certyfikatRepository,
                          WystawionyPodpisRepository wystawionyPodpisRepository,
                          DemoTsaRepository demoTsaRepository,
                          PkiProperties pkiProperties) {
        this.certyfikatRepository = certyfikatRepository;
        this.wystawionyPodpisRepository = wystawionyPodpisRepository;
        this.demoTsaRepository = demoTsaRepository;
        this.pkiProperties = pkiProperties;
    }

    public record WynikPodpisania(byte[] podpisanyPlik, String wystawionyPodpisId) {
    }

    @Transactional
    public WynikPodpisania podpiszPades(byte[] plikPdf, String certyfikatId, String dokumentReferencja) {
        CertyfikatEntity certyfikat = certyfikatRepository.findById(certyfikatId)
                .orElseThrow(() -> new NotFoundException("Certyfikat o id " + certyfikatId + " nie istnieje"));

        if (!"aktywny".equals(certyfikat.getStatus())) {
            throw new BadRequestException("Certyfikat " + certyfikatId + " nie jest aktywny (status=" + certyfikat.getStatus() + ")");
        }
        if (!certyfikat.getDataWygasniecia().isAfter(LocalDate.now())) {
            throw new BadRequestException("Certyfikat " + certyfikatId + " jest wygasly (data_wygasniecia=" + certyfikat.getDataWygasniecia() + ")");
        }
        if (certyfikat.getKeystorePkcs12() == null) {
            throw new BadRequestException("Certyfikat " + certyfikatId + " nie ma jeszcze wygenerowanego materialu kryptograficznego");
        }

        byte[] podpisanyPlik = wykonajPodpisanie(plikPdf, certyfikat);
        String znacznikCzasu = odczytajZnacznikCzasuTsa(podpisanyPlik);

        WystawionyPodpisEntity wystawionyPodpis = new WystawionyPodpisEntity();
        wystawionyPodpis.setId(UUID.randomUUID().toString());
        wystawionyPodpis.setCertyfikat(certyfikat);
        wystawionyPodpis.setDokumentReferencja(dokumentReferencja);
        wystawionyPodpis.setFormat("pades");
        wystawionyPodpis.setDataPodpisania(OffsetDateTime.now());
        wystawionyPodpis.setZnacznikCzasu(znacznikCzasu);
        wystawionyPodpis.setStatusWeryfikacji("poprawny");
        wystawionyPodpisRepository.save(wystawionyPodpis);

        return new WynikPodpisania(podpisanyPlik, wystawionyPodpis.getId());
    }

    private byte[] wykonajPodpisanie(byte[] plikPdf, CertyfikatEntity certyfikat) {
        char[] password = pkiProperties.getKeystorePassword().toCharArray();
        try (Pkcs12SignatureToken token = new Pkcs12SignatureToken(
                certyfikat.getKeystorePkcs12(), new KeyStore.PasswordProtection(password))) {
            List<DSSPrivateKeyEntry> keys = token.getKeys();
            if (keys.isEmpty()) {
                throw new BadRequestException("Keystore certyfikatu " + certyfikat.getId() + " nie zawiera kluczy");
            }
            DSSPrivateKeyEntry privateKeyEntry = keys.get(0);

            CommonCertificateVerifier certificateVerifier = buildOfflineCertificateVerifier();
            PAdESService service = new PAdESService(certificateVerifier);
            service.setTspSource(buildTspSource());

            PAdESSignatureParameters parameters = new PAdESSignatureParameters();
            parameters.setSignatureLevel(SignatureLevel.PAdES_BASELINE_T);
            parameters.setDigestAlgorithm(DigestAlgorithm.SHA256);
            parameters.setSigningCertificate(privateKeyEntry.getCertificate());
            parameters.setCertificateChain(privateKeyEntry.getCertificateChain());

            DSSDocument toSignDocument = new InMemoryDocument(plikPdf, "dokument.pdf");
            ToBeSigned dataToSign = service.getDataToSign(toSignDocument, parameters);
            SignatureValue signatureValue = token.sign(dataToSign, parameters.getDigestAlgorithm(), privateKeyEntry);
            DSSDocument signedDocument = service.signDocument(toSignDocument, parameters, signatureValue);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            signedDocument.writeTo(out);
            return out.toByteArray();
        } catch (Exception e) {
            if (e instanceof BadRequestException badRequestException) {
                throw badRequestException;
            }
            throw new BadRequestException("Nie udalo sie podpisac dokumentu: " + e.getMessage());
        }
    }

    /**
     * Zrodlo znacznikow czasu RFC 3161 oparte na materiale demo-TSA (DemoPkiInitializer) -
     * KeyEntityTSPSource (dss-spi) buduje prawdziwy TimeStampToken podpisany kluczem TSA.
     */
    private TSPSource buildTspSource() {
        DemoTsaEntity demoTsa = demoTsaRepository.findById(DEMO_TSA_ID)
                .orElseThrow(() -> new BadRequestException("Material kryptograficzny demo-TSA nie jest zainicjalizowany"));
        char[] password = pkiProperties.getKeystorePassword().toCharArray();
        KeyEntityTSPSource tspSource = new KeyEntityTSPSource(
                demoTsa.getKeystorePkcs12(), "PKCS12", password, DEMO_TSA_ALIAS, password);
        tspSource.setTsaPolicy(DEMO_TSA_POLICY_OID);
        return tspSource;
    }

    /**
     * Odczytuje genTime z pierwszego osadzonego znacznika czasu podpisu, parsujac dopiero co
     * podpisany dokument - gwarantuje zgodnosc z wartoscia faktycznie osadzona w PDF (zamiast
     * zegara aplikacji, ktory moglby sie rozjechac o rozdzielczosc sekundowa TSA).
     */
    private String odczytajZnacznikCzasuTsa(byte[] podpisanyPdf) {
        PDFDocumentValidator validator = new PDFDocumentValidator(new InMemoryDocument(podpisanyPdf));
        validator.setCertificateVerifier(buildOfflineCertificateVerifier());
        List<AdvancedSignature> signatures = validator.getSignatures();
        if (signatures.isEmpty()) {
            throw new BadRequestException("Podpisany dokument nie zawiera podpisu");
        }
        List<TimestampToken> znaczniki = signatures.get(0).getSignatureTimestamps();
        if (znaczniki.isEmpty()) {
            throw new BadRequestException("Podpisany dokument nie zawiera znacznika czasu TSA");
        }
        return znaczniki.get(0).getGenerationTime().toInstant().toString();
    }

    /** Offline z konstrukcji - brak sieciowego OCSP/CRL/AIA fetch (zakres tego slice'u). */
    public static CommonCertificateVerifier buildOfflineCertificateVerifier() {
        CommonCertificateVerifier certificateVerifier = new CommonCertificateVerifier();
        certificateVerifier.setAIASource(null);
        certificateVerifier.setOcspSource(null);
        certificateVerifier.setCrlSource(null);
        return certificateVerifier;
    }
}
