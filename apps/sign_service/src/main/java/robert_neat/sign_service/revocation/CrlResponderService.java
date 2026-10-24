package robert_neat.sign_service.revocation;

import org.bouncycastle.asn1.x509.CRLNumber;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.X509CRLHolder;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CRLConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.sign_service.entity.UniewaznienieEntity;
import robert_neat.sign_service.repository.UniewaznienieRepository;

import java.math.BigInteger;
import java.security.cert.X509CRL;
import java.util.Date;
import java.util.List;

/**
 * Logika respondera CRL (X.509v2 CRL) dla certyfikatow leaf wydanych przez issuing-CA - jedna
 * pozycja na wiersz tabeli `uniewaznienie` (ten sam zasob danych co OcspResponderService, zob.
 * jego javadoc co do roli tej tabeli jako serwerowego zaplecza respondera).
 */
@Service
public class CrlResponderService {

    // Takie samo okno waznosci jak OCSP (NEXT_UPDATE_MILLIS w OcspResponderService) - wartosc
    // demo, udokumentowana tu zamiast konfigurowalna.
    private static final long NEXT_UPDATE_MILLIS = 24L * 60 * 60 * 1000;

    private final UniewaznienieRepository uniewaznienieRepository;
    private final IssuingCaMaterial issuingCaMaterial;

    public CrlResponderService(UniewaznienieRepository uniewaznienieRepository, IssuingCaMaterial issuingCaMaterial) {
        this.uniewaznienieRepository = uniewaznienieRepository;
        this.issuingCaMaterial = issuingCaMaterial;
    }

    @Transactional(readOnly = true)
    public byte[] buildCrl() {
        try {
            IssuingCaMaterial.Material issuingCa = issuingCaMaterial.load();
            // Subject z certyfikatu issuing-CA, nie przez round-trip X509Certificate API - taki sam
            // wymog byte-identycznosci jak w DemoCertGenerator (zob. jego javadoc).
            var issuerName = new JcaX509CertificateHolder(issuingCa.certificate()).getSubject();

            Date now = new Date();
            Date nextUpdate = new Date(now.getTime() + NEXT_UPDATE_MILLIS);
            X509v2CRLBuilder builder = new X509v2CRLBuilder(issuerName, now);
            builder.setNextUpdate(nextUpdate);

            List<UniewaznienieEntity> uniewaznienia = uniewaznienieRepository.findAll();
            for (UniewaznienieEntity uniewaznienie : uniewaznienia) {
                BigInteger serialNumber = new BigInteger(uniewaznienie.getCertyfikat().getNumerSeryjny(), 16);
                Date revocationDate = Date.from(uniewaznienie.getDataUniewaznienia().toInstant());
                builder.addCRLEntry(serialNumber, revocationDate, 0);
            }

            // CRLNumber = czas epoki w sekundach - monotoniczny niezaleznie od usuwania wierszy
            // uniewaznienie (licznik wierszy NIE jest monotoniczny po ewentualnym delete).
            builder.addExtension(Extension.cRLNumber, false, new CRLNumber(BigInteger.valueOf(now.getTime() / 1000)));

            ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(issuingCa.privateKey());
            X509CRLHolder crlHolder = builder.build(signer);
            X509CRL crl = new JcaX509CRLConverter().getCRL(crlHolder);
            return crl.getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Nie udalo sie zbudowac CRL: " + e.getMessage(), e);
        }
    }
}
