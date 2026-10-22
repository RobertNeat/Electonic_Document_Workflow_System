package robert_neat.document_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** Mapowanie tabeli `sprawa`. */
@Entity
@Table(name = "sprawa")
public class SprawaEntity {

    @Id
    private String id;

    @Column(name = "znak_sprawy", nullable = false, unique = true)
    private String znakSprawy;

    @Column(nullable = false)
    private String tytul;

    @Column(nullable = false)
    private String jrwa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "definicja_obiegu_id", nullable = false)
    private DefinicjaObieguEntity definicjaObiegu;

    @Column(nullable = false)
    private String stan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "obywatel_id", nullable = false)
    private ObywatelEntity obywatel;

    @Column(nullable = false)
    private String wydzial;

    @Column(nullable = false)
    private String referent;

    @Column(name = "data_wszczecia", nullable = false)
    private LocalDate dataWszczecia;

    @Column(name = "termin_ustawowy")
    private LocalDate terminUstawowy;

    /** Wartość zgodna z CHECK constraint (ck_sprawa_kategoria_archiwalna): A, B5, B10, BE5, BE10. */
    @Column(name = "kategoria_archiwalna", nullable = false)
    private String kategoriaArchiwalna;

    @Column(nullable = false)
    private boolean zamknieta;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getZnakSprawy() {
        return znakSprawy;
    }

    public void setZnakSprawy(String znakSprawy) {
        this.znakSprawy = znakSprawy;
    }

    public String getTytul() {
        return tytul;
    }

    public void setTytul(String tytul) {
        this.tytul = tytul;
    }

    public String getJrwa() {
        return jrwa;
    }

    public void setJrwa(String jrwa) {
        this.jrwa = jrwa;
    }

    public DefinicjaObieguEntity getDefinicjaObiegu() {
        return definicjaObiegu;
    }

    public void setDefinicjaObiegu(DefinicjaObieguEntity definicjaObiegu) {
        this.definicjaObiegu = definicjaObiegu;
    }

    public String getStan() {
        return stan;
    }

    public void setStan(String stan) {
        this.stan = stan;
    }

    public ObywatelEntity getObywatel() {
        return obywatel;
    }

    public void setObywatel(ObywatelEntity obywatel) {
        this.obywatel = obywatel;
    }

    public String getWydzial() {
        return wydzial;
    }

    public void setWydzial(String wydzial) {
        this.wydzial = wydzial;
    }

    public String getReferent() {
        return referent;
    }

    public void setReferent(String referent) {
        this.referent = referent;
    }

    public LocalDate getDataWszczecia() {
        return dataWszczecia;
    }

    public void setDataWszczecia(LocalDate dataWszczecia) {
        this.dataWszczecia = dataWszczecia;
    }

    public LocalDate getTerminUstawowy() {
        return terminUstawowy;
    }

    public void setTerminUstawowy(LocalDate terminUstawowy) {
        this.terminUstawowy = terminUstawowy;
    }

    public String getKategoriaArchiwalna() {
        return kategoriaArchiwalna;
    }

    public void setKategoriaArchiwalna(String kategoriaArchiwalna) {
        this.kategoriaArchiwalna = kategoriaArchiwalna;
    }

    public boolean isZamknieta() {
        return zamknieta;
    }

    public void setZamknieta(boolean zamknieta) {
        this.zamknieta = zamknieta;
    }
}
