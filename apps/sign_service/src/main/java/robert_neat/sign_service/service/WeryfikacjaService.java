package robert_neat.sign_service.service;

import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.signature.SignatureCryptographicVerification;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.pades.validation.PDFDocumentValidator;
import eu.europa.esig.dss.spi.signature.AdvancedSignature;
import eu.europa.esig.dss.spi.x509.tsp.TimestampToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.sign_service.dto.WynikWeryfikacji;
import robert_neat.sign_service.entity.CertyfikatEntity;
import robert_neat.sign_service.entity.DemoCaEntity;
import robert_neat.sign_service.entity.DemoIssuingCaEntity;
import robert_neat.sign_service.exception.BadRequestException;
import robert_neat.sign_service.repository.CertyfikatRepository;
import robert_neat.sign_service.repository.DemoCaRepository;
import robert_neat.sign_service.repository.DemoIssuingCaRepository;
import robert_neat.sign_service.repository.UniewaznienieRepository;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Weryfikacja podpisu PAdES (BASELINE-T - znacznik czasu z mock-TSA RFC 3161 odczytywany jesli
 * obecny w podpisie, zob. odczytajZnacznikCzasuTsa; dokumenty BASELINE-B z przed tej zmiany nadal
 * weryfikowane poprawnie, tylko bez znacznika - "brak"). Bez sieciowego OCSP/CRL - uniewaznienie
 * sprawdzane WYLACZNIE lokalnie z tabeli `uniewaznienie`. Zob. WynikWeryfikacji co do rozbieznosci
 * 3- vs 4-wartosciowego statusu, ktora ten serwis rozwiazuje.
 */
@Service
public class WeryfikacjaService {

    private static final String DEMO_CA_ID = "demo-ca-1";
    private static final String DEMO_ISSUING_CA_ID = "demo-issuing-ca-1";

    private final CertyfikatRepository certyfikatRepository;
    private final UniewaznienieRepository uniewaznienieRepository;
    private final DemoCaRepository demoCaRepository;
    private final DemoIssuingCaRepository demoIssuingCaRepository;

    public WeryfikacjaService(CertyfikatRepository certyfikatRepository,
                               UniewaznienieRepository uniewaznienieRepository,
                               DemoCaRepository demoCaRepository,
                               DemoIssuingCaRepository demoIssuingCaRepository) {
        this.certyfikatRepository = certyfikatRepository;
        this.uniewaznienieRepository = uniewaznienieRepository;
        this.demoCaRepository = demoCaRepository;
        this.demoIssuingCaRepository = demoIssuingCaRepository;
    }

    @Transactional(readOnly = true)
    public WynikWeryfikacji weryfikujPades(byte[] plikPdf) {
        List<AdvancedSignature> signatures;
        try {
            PDFDocumentValidator validator = new PDFDocumentValidator(new InMemoryDocument(plikPdf));
            validator.setCertificateVerifier(PodpisService.buildOfflineCertificateVerifier());
            signatures = validator.getSignatures();
        } catch (Exception e) {
            throw new BadRequestException("Nie udalo sie odczytac pliku PDF: " + e.getMessage());
        }

        if (signatures.isEmpty()) {
            return new WynikWeryfikacji("niepoprawny", "nieznany", "nieznany", "nieznany",
                    "nieznany", "nieznany", "brak");
        }

        AdvancedSignature signature = signatures.get(0);
        signature.checkSignatureIntegrity();
        SignatureCryptographicVerification crypto = signature.getSignatureCryptographicVerification();
        boolean integralnoscOk = crypto.isSignatureValid();

        CertificateToken signingCertToken = signature.getSigningCertificateToken();
        String odciskCertyfikatu = signingCertToken != null ? sha256Fingerprint(signingCertToken.getCertificate()) : "nieznany";

        String czasPodpisu = signature.getSigningTime() != null
                ? signature.getSigningTime().toInstant().atOffset(ZoneOffset.UTC).toString()
                : Instant.EPOCH.toString();

        // Tozsamosc podpisujacego certyfikatu potwierdzona TYLKO gdy: (1) integralnosc podpisu OK,
        // (2) numer seryjny odpowiada znanemu wierszowi `certyfikat`, (3) DER certyfikatu z podpisu
        // jest bajtowo identyczny z DER zapisanym w bazie, (4) certyfikat lancuchuje sie (2 kroki:
        // leaf -> issuing-CA -> root-CA) do root-CA jako trust anchor - bez tego ostatniego
        // sprawdzenia dowolny samopodpisany certyfikat z podstawionym numerem seryjnym cert-1
        // zostalby zaakceptowany jako "poprawny".
        Optional<CertyfikatEntity> certyfikatOpt = (integralnoscOk && signingCertToken != null)
                ? znajdzZweryfikowanyCertyfikat(signingCertToken)
                : Optional.empty();

        String podpisujacy = certyfikatOpt.map(CertyfikatEntity::getPodmiot)
                .orElseGet(() -> signingCertToken != null ? signingCertToken.getSubject().getRFC2253() : "nieznany");

        String status = obliczStatus(integralnoscOk, certyfikatOpt);
        String znacznikCzasuTsa = odczytajZnacznikCzasuTsa(signature);

        return new WynikWeryfikacji(status, podpisujacy, czasPodpisu, odciskCertyfikatu,
                "nieznany", "nieznany", znacznikCzasuTsa);
    }

    /** Pierwszy znacznik czasu podpisu (BASELINE-T), jesli obecny; "brak" dla BASELINE-B. */
    private static String odczytajZnacznikCzasuTsa(AdvancedSignature signature) {
        List<TimestampToken> znaczniki = signature.getSignatureTimestamps();
        if (znaczniki.isEmpty()) {
            return "brak";
        }
        return znaczniki.get(0).getGenerationTime().toInstant().toString();
    }

    /**
     * Status finalny: brak potwierdzonej tozsamosci certyfikatu (integralnosc podpisu, DER-match,
     * lancuch leaf -> issuing-CA -> root-CA) -> niepoprawny. Inaczej kolejnosc wg kontraktu:
     * uniewazniony -> wygasly -> poprawny.
     */
    private String obliczStatus(boolean integralnoscOk, Optional<CertyfikatEntity> certyfikatOpt) {
        if (!integralnoscOk || certyfikatOpt.isEmpty()) {
            return "niepoprawny";
        }
        CertyfikatEntity certyfikat = certyfikatOpt.get();
        boolean uniewazniony = !uniewaznienieRepository.findByCertyfikat_Id(certyfikat.getId()).isEmpty();
        if (uniewazniony) {
            return "certyfikat_uniewazniony";
        }
        if (!certyfikat.getDataWygasniecia().isAfter(LocalDate.now())) {
            return "certyfikat_wygasly";
        }
        return "poprawny";
    }

    private Optional<CertyfikatEntity> znajdzZweryfikowanyCertyfikat(CertificateToken signingCertToken) {
        BigInteger serialNumber = signingCertToken.getSerialNumber();
        Optional<CertyfikatEntity> certyfikatOpt = certyfikatRepository.findAll().stream()
                .filter(c -> new BigInteger(c.getNumerSeryjny(), 16).equals(serialNumber))
                .findFirst();
        if (certyfikatOpt.isEmpty()) {
            return Optional.empty();
        }
        CertyfikatEntity certyfikat = certyfikatOpt.get();
        if (certyfikat.getCertyfikatDer() == null) {
            return Optional.empty();
        }
        byte[] signedDer;
        try {
            signedDer = signingCertToken.getEncoded();
        } catch (Exception e) {
            return Optional.empty();
        }
        if (!Arrays.equals(signedDer, certyfikat.getCertyfikatDer())) {
            return Optional.empty();
        }
        if (!podpisanyPrzezZaufanyLancuch(signingCertToken.getCertificate())) {
            return Optional.empty();
        }
        return Optional.of(certyfikat);
    }

    /**
     * Lancuch zaufania 2-poziomowy (bez sieciowej walidacji - lokalne demo-PKI, zob. plan.md):
     * leaf musi byc podpisany przez issuing-CA, a issuing-CA musi byc podpisany przez root-CA
     * (trust anchor). Oba kroki musza przejsc (AND, nie OR) - certyfikat podpisany bezposrednio
     * przez root (z pominieciem issuing-CA) nie jest prawidlowym lancuchem w tej hierarchii i musi
     * zostac odrzucony, mimo ze root jest trust anchorem.
     */
    private boolean podpisanyPrzezZaufanyLancuch(X509Certificate leafCertificate) {
        DemoCaEntity demoCa = demoCaRepository.findById(DEMO_CA_ID).orElse(null);
        DemoIssuingCaEntity demoIssuingCa = demoIssuingCaRepository.findById(DEMO_ISSUING_CA_ID).orElse(null);
        if (demoCa == null || demoIssuingCa == null) {
            return false;
        }
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            X509Certificate rootCertificate = (X509Certificate) factory.generateCertificate(
                    new java.io.ByteArrayInputStream(demoCa.getCertyfikatDer()));
            X509Certificate issuingCertificate = (X509Certificate) factory.generateCertificate(
                    new java.io.ByteArrayInputStream(demoIssuingCa.getCertyfikatDer()));
            leafCertificate.verify(issuingCertificate.getPublicKey());
            issuingCertificate.verify(rootCertificate.getPublicKey());
            return true;
        } catch (java.security.GeneralSecurityException e) {
            return false;
        }
    }

    private static String sha256Fingerprint(X509Certificate certificate) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(certificate.getEncoded());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
            return "nieznany";
        }
    }
}
