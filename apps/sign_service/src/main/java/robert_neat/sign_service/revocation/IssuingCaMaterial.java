package robert_neat.sign_service.revocation;

import org.springframework.stereotype.Component;
import robert_neat.sign_service.config.PkiProperties;
import robert_neat.sign_service.entity.DemoIssuingCaEntity;
import robert_neat.sign_service.exception.BadRequestException;
import robert_neat.sign_service.repository.DemoIssuingCaRepository;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * Wczytuje certyfikat i klucz prywatny issuing-CA z bazy (tabela `demo_issuing_ca`, material
 * wygenerowany przez DemoPkiInitializer) - wspolne dla OCSP/CRL respondera (podpisywanie
 * odpowiedzi) i dla klienta OCSP/CRL w WeryfikacjaService (weryfikacja podpisu odpowiedzi kluczem
 * publicznym issuing-CA). Nie duplikuje logiki DemoPkiInitializer.loadCertificate/
 * loadKeyPairFromPkcs12, ktora operuje tez na demo_ca/demo_tsa - tutaj wylacznie issuing-CA.
 */
@Component
public class IssuingCaMaterial {

    private static final String DEMO_ISSUING_CA_ID = "demo-issuing-ca-1";
    private static final String DEMO_ISSUING_CA_ALIAS = "demo-issuing-ca";

    private final DemoIssuingCaRepository demoIssuingCaRepository;
    private final PkiProperties pkiProperties;

    public IssuingCaMaterial(DemoIssuingCaRepository demoIssuingCaRepository, PkiProperties pkiProperties) {
        this.demoIssuingCaRepository = demoIssuingCaRepository;
        this.pkiProperties = pkiProperties;
    }

    public record Material(X509Certificate certificate, PrivateKey privateKey) {
    }

    /** @throws BadRequestException jesli material issuing-CA nie jest jeszcze zainicjalizowany. */
    public Material load() {
        DemoIssuingCaEntity entity = demoIssuingCaRepository.findById(DEMO_ISSUING_CA_ID)
                .orElseThrow(() -> new BadRequestException("Material kryptograficzny issuing-CA nie jest zainicjalizowany"));
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) factory.generateCertificate(
                    new java.io.ByteArrayInputStream(entity.getCertyfikatDer()));

            char[] password = pkiProperties.getKeystorePassword().toCharArray();
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new java.io.ByteArrayInputStream(entity.getKeystorePkcs12()), password);
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(DEMO_ISSUING_CA_ALIAS, password);
            return new Material(certificate, privateKey);
        } catch (Exception e) {
            throw new BadRequestException("Nie udalo sie wczytac materialu issuing-CA: " + e.getMessage());
        }
    }
}
