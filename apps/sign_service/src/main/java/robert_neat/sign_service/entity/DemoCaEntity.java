package robert_neat.sign_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "demo_ca")
public class DemoCaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "certyfikat_der", nullable = false)
    private byte[] certyfikatDer;

    @Column(name = "keystore_pkcs12", nullable = false)
    private byte[] keystorePkcs12;

    @Column(name = "data_utworzenia", nullable = false)
    private OffsetDateTime dataUtworzenia;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public byte[] getCertyfikatDer() {
        return certyfikatDer;
    }

    public void setCertyfikatDer(byte[] certyfikatDer) {
        this.certyfikatDer = certyfikatDer;
    }

    public byte[] getKeystorePkcs12() {
        return keystorePkcs12;
    }

    public void setKeystorePkcs12(byte[] keystorePkcs12) {
        this.keystorePkcs12 = keystorePkcs12;
    }

    public OffsetDateTime getDataUtworzenia() {
        return dataUtworzenia;
    }

    public void setDataUtworzenia(OffsetDateTime dataUtworzenia) {
        this.dataUtworzenia = dataUtworzenia;
    }
}
