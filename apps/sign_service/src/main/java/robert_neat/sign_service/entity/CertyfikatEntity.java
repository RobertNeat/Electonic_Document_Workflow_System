package robert_neat.sign_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "certyfikat")
public class CertyfikatEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "podmiot", nullable = false)
    private String podmiot;

    @Column(name = "numer_seryjny", nullable = false, unique = true)
    private String numerSeryjny;

    @Column(name = "data_wydania", nullable = false)
    private LocalDate dataWydania;

    @Column(name = "data_wygasniecia", nullable = false)
    private LocalDate dataWygasniecia;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "wydawca", nullable = false)
    private String wydawca;

    @Column(name = "certyfikat_der")
    private byte[] certyfikatDer;

    @Column(name = "keystore_pkcs12")
    private byte[] keystorePkcs12;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPodmiot() {
        return podmiot;
    }

    public void setPodmiot(String podmiot) {
        this.podmiot = podmiot;
    }

    public String getNumerSeryjny() {
        return numerSeryjny;
    }

    public void setNumerSeryjny(String numerSeryjny) {
        this.numerSeryjny = numerSeryjny;
    }

    public LocalDate getDataWydania() {
        return dataWydania;
    }

    public void setDataWydania(LocalDate dataWydania) {
        this.dataWydania = dataWydania;
    }

    public LocalDate getDataWygasniecia() {
        return dataWygasniecia;
    }

    public void setDataWygasniecia(LocalDate dataWygasniecia) {
        this.dataWygasniecia = dataWygasniecia;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getWydawca() {
        return wydawca;
    }

    public void setWydawca(String wydawca) {
        this.wydawca = wydawca;
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
}
