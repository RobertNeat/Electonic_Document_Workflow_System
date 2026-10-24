package robert_neat.sign_service.revocation;

import org.bouncycastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.ocsp.BasicOCSPRespBuilder;
import org.bouncycastle.cert.ocsp.CertificateID;
import org.bouncycastle.cert.ocsp.OCSPException;
import org.bouncycastle.cert.ocsp.OCSPReq;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.bouncycastle.cert.ocsp.OCSPRespBuilder;
import org.bouncycastle.cert.ocsp.Req;
import org.bouncycastle.cert.ocsp.RevokedStatus;
import org.bouncycastle.cert.ocsp.UnknownStatus;
import org.bouncycastle.cert.ocsp.jcajce.JcaBasicOCSPRespBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import robert_neat.sign_service.entity.CertyfikatEntity;
import robert_neat.sign_service.entity.UniewaznienieEntity;
import robert_neat.sign_service.repository.CertyfikatRepository;
import robert_neat.sign_service.repository.UniewaznienieRepository;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Logika OCSP respondera (RFC 6960) - issuing-CA podpisuje odpowiedzi SAMA (bez delegowanego
 * certyfikatu OCSP-signing), zgodnie z zakresem zadania. Tabela `uniewaznienie` jest TUTAJ
 * zrodlem prawdy o rewokacji (serwerowym zapleczem respondera) - WeryfikacjaService juz jej
 * bezposrednio nie odpytuje, zob. jego javadoc.
 */
@Service
public class OcspResponderService {

    // Okno waznosci demo-OCSP - 1 dzien, wartosc arbitralna udokumentowana tutaj (brief nie
    // wymaga konfigurowalnosci).
    private static final long NEXT_UPDATE_MILLIS = 24L * 60 * 60 * 1000;

    private final CertyfikatRepository certyfikatRepository;
    private final UniewaznienieRepository uniewaznienieRepository;
    private final IssuingCaMaterial issuingCaMaterial;

    public OcspResponderService(CertyfikatRepository certyfikatRepository,
                                 UniewaznienieRepository uniewaznienieRepository,
                                 IssuingCaMaterial issuingCaMaterial) {
        this.certyfikatRepository = certyfikatRepository;
        this.uniewaznienieRepository = uniewaznienieRepository;
        this.issuingCaMaterial = issuingCaMaterial;
    }

    /** Buduje podpisana odpowiedz OCSP DER dla zadania DER - nigdy nie wyrzuca wyjatku na blednych
     * danych wejsciowych: zle sformuowane/nieparsowalne zadanie -> MALFORMED_REQUEST (sygnalizacja
     * bledu wg samego protokolu OCSP, nie poprzez HTTP status - zob. OcspController), poprawne
     * zadanie, ale wewnetrzny blad respondera (np. material issuing-CA niezaladowany) ->
     * INTERNAL_ERROR - oba to legalne statusy OCSPResponseStatus wg RFC 6960. */
    @Transactional(readOnly = true)
    public byte[] handle(byte[] requestDer) {
        OCSPReq ocspReq;
        CertificateID certificateId;
        try {
            ocspReq = new OCSPReq(requestDer);
            Req[] requestList = ocspReq.getRequestList();
            if (requestList.length == 0) {
                return malformed();
            }
            certificateId = requestList[0].getCertID();
        } catch (Exception e) {
            return malformed();
        }

        try {
            IssuingCaMaterial.Material issuingCa = issuingCaMaterial.load();
            X509CertificateHolder issuingCaHolder = new JcaX509CertificateHolder(issuingCa.certificate());

            boolean matchesIssuer = certificateId.matchesIssuer(issuingCaHolder,
                    new JcaDigestCalculatorProviderBuilder().build());
            Optional<CertyfikatEntity> certyfikatOpt = matchesIssuer
                    ? znajdzPoNumerzeSeryjnym(certificateId.getSerialNumber())
                    : Optional.empty();

            DigestCalculator sha1DigestCalculator = new JcaDigestCalculatorProviderBuilder().build()
                    .get(CertificateID.HASH_SHA1);
            BasicOCSPRespBuilder builder = new JcaBasicOCSPRespBuilder(
                    issuingCa.certificate().getPublicKey(), sha1DigestCalculator);

            Date now = new Date();
            Date nextUpdate = new Date(now.getTime() + NEXT_UPDATE_MILLIS);

            if (certyfikatOpt.isEmpty()) {
                builder.addResponse(certificateId, new UnknownStatus(), now, nextUpdate, null);
            } else {
                CertyfikatEntity certyfikat = certyfikatOpt.get();
                List<UniewaznienieEntity> uniewaznienia = uniewaznienieRepository.findByCertyfikat_Id(certyfikat.getId());
                if (!uniewaznienia.isEmpty()) {
                    Date revocationDate = Date.from(uniewaznienia.get(0).getDataUniewaznienia().toInstant());
                    builder.addResponse(certificateId, new RevokedStatus(revocationDate, 0), now, nextUpdate, null);
                } else {
                    builder.addResponse(certificateId, null, now, nextUpdate, null);
                }
            }

            echoNonce(ocspReq, builder);

            ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(issuingCa.privateKey());
            var basicResp = builder.build(signer, new X509CertificateHolder[]{issuingCaHolder}, now);
            OCSPRespBuilder respBuilder = new OCSPRespBuilder();
            OCSPResp ocspResp = respBuilder.build(OCSPRespBuilder.SUCCESSFUL, basicResp);
            return ocspResp.getEncoded();
        } catch (Exception e) {
            return internalError();
        }
    }

    private void echoNonce(OCSPReq ocspReq, BasicOCSPRespBuilder builder) {
        if (!ocspReq.hasExtensions()) {
            return;
        }
        Extension nonceExtension = ocspReq.getExtension(OCSPObjectIdentifiers.id_pkix_ocsp_nonce);
        if (nonceExtension == null) {
            return;
        }
        builder.setResponseExtensions(new Extensions(new Extension(
                OCSPObjectIdentifiers.id_pkix_ocsp_nonce, false, nonceExtension.getExtnValue())));
    }

    private Optional<CertyfikatEntity> znajdzPoNumerzeSeryjnym(BigInteger serialNumber) {
        return certyfikatRepository.findAll().stream()
                .filter(c -> new BigInteger(c.getNumerSeryjny(), 16).equals(serialNumber))
                .findFirst();
    }

    private static byte[] malformed() {
        return buildErrorResponse(OCSPRespBuilder.MALFORMED_REQUEST);
    }

    private static byte[] internalError() {
        return buildErrorResponse(OCSPRespBuilder.INTERNAL_ERROR);
    }

    private static byte[] buildErrorResponse(int ocspResponseStatus) {
        try {
            return new OCSPRespBuilder().build(ocspResponseStatus, null).getEncoded();
        } catch (OCSPException | java.io.IOException e) {
            throw new IllegalStateException("Nie udalo sie zbudowac odpowiedzi bledu OCSP", e);
        }
    }
}
