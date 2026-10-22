package robert_neat.document_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** Mapowanie tabeli `wpis_statusu_przesylki` - historia zmian statusu przesyłki. */
@Entity
@Table(name = "wpis_statusu_przesylki")
public class WpisStatusuPrzesylkiEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "przesylka_id", nullable = false)
    private PrzesylkaEntity przesylka;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private OffsetDateTime data;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public PrzesylkaEntity getPrzesylka() {
        return przesylka;
    }

    public void setPrzesylka(PrzesylkaEntity przesylka) {
        this.przesylka = przesylka;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getData() {
        return data;
    }

    public void setData(OffsetDateTime data) {
        this.data = data;
    }
}
