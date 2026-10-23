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
@Table(name = "uniewaznienie")
public class UniewaznienieEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certyfikat_id", nullable = false)
    private CertyfikatEntity certyfikat;

    @Column(name = "data_uniewaznienia", nullable = false)
    private OffsetDateTime dataUniewaznienia;

    @Column(name = "powod", nullable = false)
    private String powod;

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

    public OffsetDateTime getDataUniewaznienia() {
        return dataUniewaznienia;
    }

    public void setDataUniewaznienia(OffsetDateTime dataUniewaznienia) {
        this.dataUniewaznienia = dataUniewaznienia;
    }

    public String getPowod() {
        return powod;
    }

    public void setPowod(String powod) {
        this.powod = powod;
    }
}
