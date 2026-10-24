package robert_neat.sign_service.service;

import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.model.signature.SignatureCryptographicVerification;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.pades.validation.PDFDocumentValidator;
import eu.europa.esig.dss.spi.signature.AdvancedSignature;
import eu.europa.esig.dss.spi.x509.tsp.TimestampToken;
import eu.europa.esig.dss.xades.validation.XMLDocumentValidator;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.springframework.stereotype.Service;
import robert_neat.sign_service.dto.WynikWeryfikacji;
import robert_neat.sign_service.entity.CertyfikatEntity;
import robert_neat.sign_service.entity.DemoCaEntity;
import robert_neat.sign_service.entity.DemoIssuingCaEntity;
import robert_neat.sign_service.exception.BadRequestException;
import robert_neat.sign_service.repository.CertyfikatRepository;
import robert_neat.sign_service.repository.DemoCaRepository;
import robert_neat.sign_service.repository.DemoIssuingCaRepository;
import robert_neat.sign_service.revocation.CrlClient;
import robert_neat.sign_service.revocation.IssuingCaMaterial;
import robert_neat.sign_service.revocation.OcspClient;

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
 * Weryfikacja podpisu PAdES (PDF) i XAdES (XML), oba BASELINE-T - znacznik czasu z mock-TSA
 * RFC 3161 odczytywany jesli obecny w podpisie, zob. odczytajZnacznikCzasuTsa; dokumenty
 * BASELINE-B z przed tej zmiany nadal weryfikowane poprawnie, tylko bez znacznika - "brak").
 * Status unieważnienia jest ustalany przez PRAWDZIWE wywolanie sieciowe: OCSP (adres odczytany z
 * rozszerzenia AuthorityInfoAccess certyfikatu podpisujacego), a jesli OCSP zawiedzie/jest
 * nieosiagalny - fallback na CRL (adres z CRLDistributionPoints). Obie odpowiedzi sa
 * kryptograficznie weryfikowane kluczem publicznym issuing-CA przed zaufaniem ich trescia (zob.
 * OcspClient/CrlClient) - nieautentyczna lub niedostepna odpowiedz NIE jest traktowana jako
 * "good". Jesli OBA kanaly zawiedzie, wynik jest fail-closed: status koncowy to "niepoprawny"
 * (zob. obliczStatus) - serwis NIE spada z powrotem na lokalna tabele `uniewaznienie` (ta tabela
 * jest teraz wylacznie serwerowym zapleczem respondera OCSP/CRL, zob.
 * robert_neat.sign_service.revocation). Zob. WynikWeryfikacji co do rozbieznosci 3- vs
 * 4-wartosciowego statusu, ktora ten serwis rozwiazuje. Logika rozpoznawania tozsamosci
 * certyfikatu/lancucha zaufania (znajdzZweryfikowanyCertyfikat, podpisanyPrzezZaufanyLancuch)
 * operuje na typach DSS niezaleznych od formatu (AdvancedSignature, CertificateToken), wiec jest
 * wspolna dla PAdES i XAdES bez duplikacji.
 */
@Service
public class WeryfikacjaService {

    private static final String DEMO_CA_ID = "demo-ca-1";
    private static final String DEMO_ISSUING_CA_ID = "demo-issuing-ca-1";

    private final CertyfikatRepository certyfikatRepository;
    private final DemoCaRepository demoCaRepository;
    private final DemoIssuingCaRepository demoIssuingCaRepository;
    private final IssuingCaMaterial issuingCaMaterial;
    private final OcspClient ocspClient;
    private final CrlClient crlClient;

    public WeryfikacjaService(CertyfikatRepository certyfikatRepository,
                               DemoCaRepository demoCaRepository,
                               DemoIssuingCaRepository demoIssuingCaRepository,
                               IssuingCaMaterial issuingCaMaterial,
                               OcspClient ocspClient,
                               CrlClient crlClient) {
        this.certyfikatRepository = certyfikatRepository;
        this.demoCaRepository = demoCaRepository;
        this.demoIssuingCaRepository = demoIssuingCaRepository;
        this.issuingCaMaterial = issuingCaMaterial;
        this.ocspClient = ocspClient;
        this.crlClient = crlClient;
    }

    public WynikWeryfikacji weryfikujPades(byte[] plikPdf) {
        List<AdvancedSignature> signatures;
        try {
            PDFDocumentValidator validator = new PDFDocumentValidator(new InMemoryDocument(plikPdf));
            validator.setCertificateVerifier(PodpisService.buildOfflineCertificateVerifier());
            signatures = validator.getSignatures();
        } catch (Exception e) {
            throw new BadRequestException("Nie udalo sie odczytac pliku PDF: " + e.getMessage());
        }
        return zweryfikujPodpisy(signatures);
    }

    public WynikWeryfikacji weryfikujXades(byte[] plikXml) {
        List<AdvancedSignature> signatures;
        try {
            XMLDocumentValidator validator = new XMLDocumentValidator(new InMemoryDocument(plikXml));
            validator.setCertificateVerifier(PodpisService.buildOfflineCertificateVerifier());
            signatures = validator.getSignatures();
        } catch (Exception e) {
            throw new BadRequestException("Nie udalo sie odczytac pliku XML: " + e.getMessage());
        }
        return zweryfikujPodpisy(signatures);
    }

    /**
     * Wspolna ocena podpisu (integralnosc, tozsamosc certyfikatu, status, znacznik TSA) - operuje
     * wylacznie na AdvancedSignature, niezaleznie od tego, czy sygnatury pochodza z PDF (PAdES) czy
     * z XML (XAdES).
     */
    private WynikWeryfikacji zweryfikujPodpisy(List<AdvancedSignature> signatures) {
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

        String status = obliczStatus(integralnoscOk, certyfikatOpt,
                certyfikatOpt.isPresent() && signingCertToken != null ? signingCertToken.getCertificate() : null);
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
     * uniewazniony -> (nieokreslony/niedostepny OCSP+CRL) -> wygasly -> poprawny. Status
     * unieważnienia pochodzi WYLACZNIE z sieciowego OCSP (fallback CRL) - zob. ustalStatusRewokacji;
     * gdy OBA kanaly zawiedza, wynik jest fail-closed ("niepoprawny"), nigdy lokalna tabela
     * `uniewaznienie` (ta jest teraz tylko zapleczem respondera, zob. javadoc klasy).
     */
    private String obliczStatus(boolean integralnoscOk, Optional<CertyfikatEntity> certyfikatOpt, X509Certificate leafCertificate) {
        if (!integralnoscOk || certyfikatOpt.isEmpty()) {
            return "niepoprawny";
        }
        CertyfikatEntity certyfikat = certyfikatOpt.get();

        Optional<Boolean> uniewazniony = ustalStatusRewokacji(leafCertificate);
        if (uniewazniony.isEmpty()) {
            // Fail-closed: OCSP niedostepny/nieautentyczny I CRL niedostepny/nieautentyczny -
            // nie da sie potwierdzic, ze certyfikat NIE jest uniewazniony, wiec caly podpis jest
            // traktowany jako niepoprawny (nie wolno zglosic "poprawny" bez tego potwierdzenia).
            return "niepoprawny";
        }
        if (uniewazniony.get()) {
            return "certyfikat_uniewazniony";
        }
        if (!certyfikat.getDataWygasniecia().isAfter(LocalDate.now())) {
            return "certyfikat_wygasly";
        }
        return "poprawny";
    }

    /**
     * OCSP najpierw (adres z AuthorityInfoAccess certyfikatu), CRL jako fallback (adres z
     * CRLDistributionPoints) - zob. javadoc klasy co do fail-closed semantyki. @return
     * Optional.empty() jesli status rewokacji nie mogl zostac wiarygodnie ustalony (OBA kanaly
     * zawiodly), inaczej true=uniewazniony/false=nieuniewazniony wg pierwszego kanalu, ktory dal
     * wiarygodna odpowiedz.
     */
    private Optional<Boolean> ustalStatusRewokacji(X509Certificate leafCertificate) {
        if (leafCertificate == null) {
            return Optional.empty();
        }
        X509Certificate issuingCaCertificate = issuingCaMaterial.load().certificate();

        String ocspUrl = odczytajOcspUrl(leafCertificate);
        if (ocspUrl != null) {
            Optional<OcspClient.Status> ocspStatus = ocspClient.sprawdz(ocspUrl, issuingCaCertificate, leafCertificate);
            if (ocspStatus.isPresent() && ocspStatus.get() != OcspClient.Status.UNKNOWN) {
                return Optional.of(ocspStatus.get() == OcspClient.Status.REVOKED);
            }
        }

        String crlUrl = odczytajCrlUrl(leafCertificate);
        if (crlUrl != null) {
            Optional<Boolean> crlResult = crlClient.jestUniewazniony(crlUrl, issuingCaCertificate, leafCertificate.getSerialNumber());
            if (crlResult.isPresent()) {
                return crlResult;
            }
        }

        return Optional.empty();
    }

    /** Pierwszy URI OCSP z rozszerzenia AuthorityInfoAccess, jesli obecne; null jesli brak. */
    private static String odczytajOcspUrl(X509Certificate certificate) {
        try {
            byte[] extensionValue = certificate.getExtensionValue(Extension.authorityInfoAccess.getId());
            if (extensionValue == null) {
                return null;
            }
            ASN1Sequence sequence = (ASN1Sequence) org.bouncycastle.asn1.ASN1Primitive.fromByteArray(
                    org.bouncycastle.asn1.DEROctetString.getInstance(extensionValue).getOctets());
            AuthorityInformationAccess aia = AuthorityInformationAccess.getInstance(sequence);
            for (AccessDescription accessDescription : aia.getAccessDescriptions()) {
                if (accessDescription.getAccessMethod().equals(X509ObjectIdentifiers.ocspAccessMethod)) {
                    GeneralName location = accessDescription.getAccessLocation();
                    if (location.getTagNo() == GeneralName.uniformResourceIdentifier) {
                        return location.getName().toString();
                    }
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Pierwszy URI z pierwszego DistributionPoint rozszerzenia CRLDistributionPoints, jesli obecne. */
    private static String odczytajCrlUrl(X509Certificate certificate) {
        try {
            byte[] extensionValue = certificate.getExtensionValue(Extension.cRLDistributionPoints.getId());
            if (extensionValue == null) {
                return null;
            }
            ASN1Sequence sequence = (ASN1Sequence) org.bouncycastle.asn1.ASN1Primitive.fromByteArray(
                    org.bouncycastle.asn1.DEROctetString.getInstance(extensionValue).getOctets());
            CRLDistPoint crlDistPoint = CRLDistPoint.getInstance(sequence);
            for (DistributionPoint distributionPoint : crlDistPoint.getDistributionPoints()) {
                DistributionPointName distributionPointName = distributionPoint.getDistributionPoint();
                if (distributionPointName == null || distributionPointName.getType() != DistributionPointName.FULL_NAME) {
                    continue;
                }
                GeneralNames generalNames = GeneralNames.getInstance(distributionPointName.getName());
                for (GeneralName generalName : generalNames.getNames()) {
                    if (generalName.getTagNo() == GeneralName.uniformResourceIdentifier) {
                        return generalName.getName().toString();
                    }
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
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
     * Lancuch zaufania 2-poziomowy (ta czesc nadal lokalna/offline - podpisanie certyfikatow leaf
     * przez issuing-CA jest ustalone raz w momencie generowania materialu demo-PKI, nie wymaga
     * sieciowego sprawdzenia; sieciowy OCSP/CRL jest potrzebny WYLACZNIE do statusu unieważnienia,
     * zob. ustalStatusRewokacji): leaf musi byc podpisany przez issuing-CA, a issuing-CA musi byc podpisany przez root-CA
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
