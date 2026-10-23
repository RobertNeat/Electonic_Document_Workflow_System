package robert_neat.sign_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sign.pki")
public class PkiProperties {

    /** Haslo chroniace PKCS12 keystore'y demo-CA i certyfikatow leaf (dev-default w application.properties). */
    private String keystorePassword;

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }
}
