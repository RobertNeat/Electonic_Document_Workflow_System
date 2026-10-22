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

/** Mapowanie tabeli `naleznosc`. Pole `status` przechowywane jako string DB-value
 * (CHECK ck_naleznosc_status: nierozliczona/czesciowo_rozliczona/rozliczona/przeterminowana),
 * NIE @Enumerated(STRING) - patrz uwaga w ObywatelEntity/DokumentEntity dla analogicznego powodu. */
@Entity
@Table(name = "naleznosc")
public class NaleznoscEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprawa_id", nullable = false)
    private SprawaEntity sprawa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "obywatel_id", nullable = false)
    private ObywatelEntity obywatel;

    @Column(name = "numer_referencyjny", nullable = false, unique = true)
    private String numerReferencyjny;

    @Column(nullable = false)
    private String tytul;

    @Column(nullable = false)
    private BigDecimal kwota;

    @Column(name = "kwota_rozliczona", nullable = false)
    private BigDecimal kwotaRozliczona;

    @Column(name = "termin_platnosci", nullable = false)
    private LocalDate terminPlatnosci;

    @Column(nullable = false)
    private String status;

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

    public ObywatelEntity getObywatel() {
        return obywatel;
    }

    public void setObywatel(ObywatelEntity obywatel) {
        this.obywatel = obywatel;
    }

    public String getNumerReferencyjny() {
        return numerReferencyjny;
    }

    public void setNumerReferencyjny(String numerReferencyjny) {
        this.numerReferencyjny = numerReferencyjny;
    }

    public String getTytul() {
        return tytul;
    }

    public void setTytul(String tytul) {
        this.tytul = tytul;
    }

    public BigDecimal getKwota() {
        return kwota;
    }

    public void setKwota(BigDecimal kwota) {
        this.kwota = kwota;
    }

    public BigDecimal getKwotaRozliczona() {
        return kwotaRozliczona;
    }

    public void setKwotaRozliczona(BigDecimal kwotaRozliczona) {
        this.kwotaRozliczona = kwotaRozliczona;
    }

    public LocalDate getTerminPlatnosci() {
        return terminPlatnosci;
    }

    public void setTerminPlatnosci(LocalDate terminPlatnosci) {
        this.terminPlatnosci = terminPlatnosci;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
