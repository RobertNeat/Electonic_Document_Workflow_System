package robert_neat.document_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** Mapowanie tabeli `szablon_jasper`. Treść .jrxml żyje w jasper_service - tu tylko metadane. */
@Entity
@Table(name = "szablon_jasper")
public class SzablonJasperEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "typ_dokumentu_id", nullable = false)
    private TypDokumentuEntity typDokumentu;

    @Column(nullable = false)
    private String nazwa;

    @Column(nullable = false)
    private int wersja;

    @Column(name = "data_wgrania", nullable = false)
    private LocalDate dataWgrania;

    @Column(nullable = false)
    private String autor;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TypDokumentuEntity getTypDokumentu() {
        return typDokumentu;
    }

    public void setTypDokumentu(TypDokumentuEntity typDokumentu) {
        this.typDokumentu = typDokumentu;
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
}
