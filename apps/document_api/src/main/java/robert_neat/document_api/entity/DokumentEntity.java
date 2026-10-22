package robert_neat.document_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** Mapowanie tabeli `dokument`. Treść plików żyje w dms_service - tu tylko metadane. */
@Entity
@Table(name = "dokument")
public class DokumentEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprawa_id", nullable = false)
    private SprawaEntity sprawa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "typ_dokumentu_id", nullable = false)
    private TypDokumentuEntity typDokumentu;

    @Column(nullable = false)
    private String nazwa;

    @Column(name = "aktualna_wersja", nullable = false)
    private int aktualnaWersja;

    @Column(name = "data_utworzenia", nullable = false)
    private LocalDate dataUtworzenia;

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

    public int getAktualnaWersja() {
        return aktualnaWersja;
    }

    public void setAktualnaWersja(int aktualnaWersja) {
        this.aktualnaWersja = aktualnaWersja;
    }

    public LocalDate getDataUtworzenia() {
        return dataUtworzenia;
    }

    public void setDataUtworzenia(LocalDate dataUtworzenia) {
        this.dataUtworzenia = dataUtworzenia;
    }
}
