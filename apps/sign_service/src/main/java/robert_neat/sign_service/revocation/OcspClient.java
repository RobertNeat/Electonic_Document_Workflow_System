package robert_neat.sign_service.revocation;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.ocsp.BasicOCSPResp;
import org.bouncycastle.cert.ocsp.CertificateID;
import org.bouncycastle.cert.ocsp.CertificateStatus;
import org.bouncycastle.cert.ocsp.OCSPReq;
import org.bouncycastle.cert.ocsp.OCSPReqBuilder;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.bouncycastle.cert.ocsp.RevokedStatus;
import org.bouncycastle.cert.ocsp.SingleResp;
import org.bouncycastle.cert.ocsp.UnknownStatus;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.security.cert.X509Certificate;
import java.util.Optional;

/**
 * Klient OCSP (RFC 6960) uzywany przez WeryfikacjaService - adres respondera jest odczytywany Z
 * SAMEGO certyfikatu podpisujacego (rozszerzenie AuthorityInfoAccess), nie hardkodowany drugi raz
 * tutaj, zgodnie z tym jak dziala realny klient OCSP. Odpowiedz jest NIEUFNA, dopoki jej podpis
 * nie zostanie zweryfikowany kluczem publicznym issuing-CA (authenticity check, zob. sprawdz()) -
 * fail-closed: kazdy blad (siec, parsowanie, podpis, niezgodnosc CertificateID) zwraca
 * Optional.empty(), NIGDY nie jest interpretowany jako "good".
 */
@Component
public class OcspClient {

    public enum Status {GOOD, REVOKED, UNKNOWN}

    private final RestClient restClient;

    public OcspClient(RestClient.Builder revocationRestClientBuilder) {
        this.restClient = revocationRestClientBuilder.build();
    }

    /** @return Optional.empty() jesli wywolanie siecowe/parsowanie/weryfikacja podpisu zawiodly -
     * fail-closed, NIE "good" domyslnie. */
    public Optional<Status> sprawdz(String ocspUrl, X509Certificate issuingCaCertificate, X509Certificate leafCertificate) {
        try {
            X509CertificateHolder issuingCaHolder = new JcaX509CertificateHolder(issuingCaCertificate);
            var digestCalculator = new JcaDigestCalculatorProviderBuilder().build().get(CertificateID.HASH_SHA1);
            CertificateID certificateId = new CertificateID(digestCalculator, issuingCaHolder, leafCertificate.getSerialNumber());

            OCSPReqBuilder reqBuilder = new OCSPReqBuilder();
            reqBuilder.addRequest(certificateId);
            OCSPReq ocspReq = reqBuilder.build();

            byte[] responseBytes = restClient.post()
                    .uri(ocspUrl)
                    .contentType(MediaType.valueOf("application/ocsp-request"))
                    .body(ocspReq.getEncoded())
                    .retrieve()
                    .body(byte[].class);
            if (responseBytes == null) {
                return Optional.empty();
            }

            OCSPResp ocspResp = new OCSPResp(responseBytes);
            if (ocspResp.getStatus() != OCSPResp.SUCCESSFUL) {
                return Optional.empty();
            }
            BasicOCSPResp basicResp = (BasicOCSPResp) ocspResp.getResponseObject();
            boolean signatureValid = basicResp.isSignatureValid(
                    new JcaContentVerifierProviderBuilder().build(issuingCaCertificate.getPublicKey()));
            if (!signatureValid) {
                return Optional.empty();
            }

            SingleResp[] responses = basicResp.getResponses();
            if (responses.length == 0) {
                return Optional.empty();
            }
            SingleResp singleResp = responses[0];
            if (!singleResp.getCertID().equals(certificateId)) {
                return Optional.empty();
            }

            CertificateStatus certStatus = singleResp.getCertStatus();
            if (certStatus == null) {
                return Optional.of(Status.GOOD);
            }
            if (certStatus instanceof RevokedStatus) {
                return Optional.of(Status.REVOKED);
            }
            if (certStatus instanceof UnknownStatus) {
                return Optional.of(Status.UNKNOWN);
            }
            return Optional.empty();
        } catch (java.io.IOException | java.security.cert.CertificateEncodingException
                 | org.bouncycastle.cert.ocsp.OCSPException
                 | org.bouncycastle.operator.OperatorCreationException | RuntimeException e) {
            // RuntimeException zakres obejmuje RestClientException (siec/HTTP) - wyjatek zlapany
            // tutaj szeroko, bo KAZDY blad przy ustalaniu statusu OCSP musi fail-closed do
            // Optional.empty(), nigdy nie przeciekac jako wyjatek do wolajacego.
            return Optional.empty();
        }
    }
}
