package robert_neat.document_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** Mapowanie tabeli `wersja_dokumentu`. Pole `statusPodpisu` przechowywane jako string DB-value
 * (CHECK constraint: brak/poprawny/niepoprawny/certyfikat_wygasly/certyfikat_uniewazniony) -
 * analogicznie do ObywatelEntity.typ / SprawaEntity.kategoriaArchiwalna, żeby nie zapisywać
 * nazwy stałej enuma Java zamiast wartości DB (zob. StatusPodpisu.getWartosc()). */
@Entity
@Table(name = "wersja_dokumentu")
public class WersjaDokumentuEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dokument_id", nullable = false)
    private DokumentEntity dokument;

    @Column(nullable = false)
    private int wersja;

    @Column(nullable = false)
    private boolean podpisana;

    @Column(name = "data_utworzenia", nullable = false)
    private LocalDate dataUtworzenia;

    @Column(nullable = false)
    private String autor;

    @Column(name = "status_podpisu", nullable = false)
    private String statusPodpisu;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public DokumentEntity getDokument() {
        return dokument;
    }

    public void setDokument(DokumentEntity dokument) {
        this.dokument = dokument;
    }

    public int getWersja() {
        return wersja;
    }

    public void setWersja(int wersja) {
        this.wersja = wersja;
    }

    public boolean isPodpisana() {
        return podpisana;
    }

    public void setPodpisana(boolean podpisana) {
        this.podpisana = podpisana;
    }

    public LocalDate getDataUtworzenia() {
        return dataUtworzenia;
    }

    public void setDataUtworzenia(LocalDate dataUtworzenia) {
        this.dataUtworzenia = dataUtworzenia;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getStatusPodpisu() {
        return statusPodpisu;
    }

    public void setStatusPodpisu(String statusPodpisu) {
        this.statusPodpisu = statusPodpisu;
    }
}
