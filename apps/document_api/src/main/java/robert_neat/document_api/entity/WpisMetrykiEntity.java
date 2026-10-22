package robert_neat.document_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** Mapowanie tabeli `wpis_metryki`. */
@Entity
@Table(name = "wpis_metryki")
public class WpisMetrykiEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprawa_id", nullable = false)
    private SprawaEntity sprawa;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private String czynnosc;

    @Column(nullable = false)
    private String osoba;

    private String opis;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SprawaEntity getSprawa() {
        return sprawa;
    }

    public void setSprawa(SprawaEntity sprawa) {
        this.sprawa = sprawa;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public String getCzynnosc() {
        return czynnosc;
    }

    public void setCzynnosc(String czynnosc) {
        this.czynnosc = czynnosc;
    }

    public String getOsoba() {
        return osoba;
    }

    public void setOsoba(String osoba) {
        this.osoba = osoba;
    }

    public String getOpis() {
        return opis;
    }

    public void setOpis(String opis) {
        this.opis = opis;
    }
}
