package robert_neat.dms_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** Mapowanie tabeli `szablon_jasper`. `typDokumentuId` jest płaskim varchar - referencja do
 * core.typ_dokumentu.id bez FK międzyschemowego (zob. komentarz w changelogu 001). */
@Entity
@Table(name = "szablon_jasper")
public class SzablonJasperEntity {

    @Id
    private String id;

    @Column(name = "typ_dokumentu_id", nullable = false)
    private String typDokumentuId;

    @Column(nullable = false)
    private String nazwa;

    @Column(nullable = false)
    private int wersja;

    @Column(name = "data_wgrania", nullable = false)
    private LocalDate dataWgrania;

    @Column(nullable = false)
    private String autor;

    @Column(name = "sciezka_pliku", nullable = false)
    private String sciezkaPliku;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTypDokumentuId() {
        return typDokumentuId;
    }

    public void setTypDokumentuId(String typDokumentuId) {
        this.typDokumentuId = typDokumentuId;
    }

    public String getNazwa() {
        return nazwa;
    }

    public void setNazwa(String nazwa) {
        this.nazwa = nazwa;
    }

    public int getWersja() {
        return wersja;
    }

    public void setWersja(int wersja) {
        this.wersja = wersja;
    }

    public LocalDate getDataWgrania() {
        return dataWgrania;
    }

    public void setDataWgrania(LocalDate dataWgrania) {
        this.dataWgrania = dataWgrania;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getSciezkaPliku() {
        return sciezkaPliku;
    }

    public void setSciezkaPliku(String sciezkaPliku) {
        this.sciezkaPliku = sciezkaPliku;
    }
}
