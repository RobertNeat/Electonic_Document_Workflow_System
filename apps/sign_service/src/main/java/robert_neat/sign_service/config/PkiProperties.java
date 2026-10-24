package robert_neat.sign_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sign.pki")
public class PkiProperties {

    /** Haslo chroniace PKCS12 keystore'y demo-CA i certyfikatow leaf (dev-default w application.properties). */
    private String keystorePassword;

    /**
     * Publiczny adres bazowy tego serwisu, pod ktorym inne strony (klienci OCSP/CRL) moga je
     * odpytac - uzywany WYLACZNIE do zbudowania URL-i AIA/CDP zapisanych w rozszerzeniach
     * certyfikatow leaf w momencie ich generowania (zob. DemoCertGenerator.generateLeafCertificate).
     * Zmiana tej wartosci NIE aktualizuje juz wygenerowanych certyfikatow - wymaga ponownego
     * wygenerowania materialu leaf (zob. migracja 005).
     */
    private String publicBaseUrl;

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }
}
