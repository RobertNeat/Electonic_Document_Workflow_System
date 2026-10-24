package robert_neat.sign_service.revocation;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Optional;

/**
 * Klient CRL (CDP), uzywany jako fallback przez WeryfikacjaService gdy OCSP jest niedostepny -
 * adres pochodzi z rozszerzenia CRLDistributionPoints certyfikatu podpisujacego. Fail-closed tak
 * jak OcspClient: podpis CRL musi zweryfikowac sie kluczem publicznym issuing-CA, inaczej wynik to
 * Optional.empty() (nie "brak wpisu = niereodwolany").
 */
@Component
public class CrlClient {

    private final RestClient restClient;

    public CrlClient(RestClient.Builder revocationRestClientBuilder) {
        this.restClient = revocationRestClientBuilder.build();
    }

    /** @return Optional.empty() jesli CRL niedostepny/niepoprawny/nieautentyczny, inaczej true/false
     * czy podany numer seryjny jest na liscie unieważnionych. */
    public Optional<Boolean> jestUniewazniony(String crlUrl, X509Certificate issuingCaCertificate, BigInteger serialNumber) {
        try {
            byte[] crlBytes = restClient.get()
                    .uri(crlUrl)
                    .retrieve()
                    .body(byte[].class);
            if (crlBytes == null) {
                return Optional.empty();
            }

            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            X509CRL crl = (X509CRL) factory.generateCRL(new java.io.ByteArrayInputStream(crlBytes));

            try {
                crl.verify(issuingCaCertificate.getPublicKey());
            } catch (java.security.GeneralSecurityException e) {
                return Optional.empty();
            }

            boolean revoked = crl.getRevokedCertificate(serialNumber) != null;
            return Optional.of(revoked);
        } catch (CertificateException | java.security.cert.CRLException | RuntimeException e) {
            // RuntimeException zakres obejmuje RestClientException (siec/HTTP) - fail-closed na
            // KAZDY blad, zob. OcspClient dla tego samego wzorca.
            return Optional.empty();
        }
    }
}
