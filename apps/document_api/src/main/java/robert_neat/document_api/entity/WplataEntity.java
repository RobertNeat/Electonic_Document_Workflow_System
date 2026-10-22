package robert_neat.document_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Mapowanie tabeli `wplata`. FK `naleznosc_id` jest nullable - wpłata może istnieć
 * niezidentyfikowana, czekając na ręczną lub automatyczną rekoncyliację. */
@Entity
@Table(name = "wplata")
public class WplataEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "naleznosc_id")
    private NaleznoscEntity naleznosc;

    @Column(nullable = false)
    private BigDecimal kwota;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private String tytul;

    @Column(nullable = false)
    private String kontrahent;

    @Column(name = "dopasowana_automatycznie", nullable = false)
    private boolean dopasowanaAutomatycznie;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public NaleznoscEntity getNaleznosc() {
        return naleznosc;
    }

    public void setNaleznosc(NaleznoscEntity naleznosc) {
        this.naleznosc = naleznosc;
    }

    public BigDecimal getKwota() {
        return kwota;
    }

    public void setKwota(BigDecimal kwota) {
        this.kwota = kwota;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public String getTytul() {
        return tytul;
    }

    public void setTytul(String tytul) {
        this.tytul = tytul;
    }

    public String getKontrahent() {
        return kontrahent;
    }

    public void setKontrahent(String kontrahent) {
        this.kontrahent = kontrahent;
    }

    public boolean isDopasowanaAutomatycznie() {
        return dopasowanaAutomatycznie;
    }

    public void setDopasowanaAutomatycznie(boolean dopasowanaAutomatycznie) {
        this.dopasowanaAutomatycznie = dopasowanaAutomatycznie;
    }
}
