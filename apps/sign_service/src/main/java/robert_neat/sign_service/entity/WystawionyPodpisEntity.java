package robert_neat.sign_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "wystawiony_podpis")
public class WystawionyPodpisEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certyfikat_id", nullable = false)
    private CertyfikatEntity certyfikat;

    @Column(name = "dokument_referencja", nullable = false)
    private String dokumentReferencja;

    @Column(name = "format", nullable = false)
    private String format;

    @Column(name = "data_podpisania", nullable = false)
    private OffsetDateTime dataPodpisania;

    @Column(name = "znacznik_czasu", nullable = false)
    private String znacznikCzasu;

    @Column(name = "status_weryfikacji", nullable = false)
    private String statusWeryfikacji;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public CertyfikatEntity getCertyfikat() {
        return certyfikat;
    }

    public void setCertyfikat(CertyfikatEntity certyfikat) {
        this.certyfikat = certyfikat;
    }

    public String getDokumentReferencja() {
        return dokumentReferencja;
    }

    public void setDokumentReferencja(String dokumentReferencja) {
        this.dokumentReferencja = dokumentReferencja;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public OffsetDateTime getDataPodpisania() {
        return dataPodpisania;
    }

    public void setDataPodpisania(OffsetDateTime dataPodpisania) {
        this.dataPodpisania = dataPodpisania;
    }

    public String getZnacznikCzasu() {
        return znacznikCzasu;
    }

    public void setZnacznikCzasu(String znacznikCzasu) {
        this.znacznikCzasu = znacznikCzasu;
    }

    public String getStatusWeryfikacji() {
        return statusWeryfikacji;
    }

    public void setStatusWeryfikacji(String statusWeryfikacji) {
        this.statusWeryfikacji = statusWeryfikacji;
    }
}
