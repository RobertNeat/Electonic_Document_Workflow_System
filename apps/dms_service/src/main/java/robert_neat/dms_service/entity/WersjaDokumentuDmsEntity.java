package robert_neat.dms_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/** Mapowanie tabeli `wersja_dokumentu_dms`. `dokumentId` jest płaskim varchar - referencja do
 * core.dokument.id bez FK międzyschemowego. `statusPodpisu` przechowywany jako String (CHECK
 * constraint w bazie: brak/poprawny/niepoprawny/certyfikat_wygasly/certyfikat_uniewazniony),
 * bez @Enumerated - zgodnie z konwencją document_api. */
@Entity
@Table(name = "wersja_dokumentu_dms")
public class WersjaDokumentuDmsEntity {

    @Id
    private String id;

    @Column(name = "dokument_id", nullable = false)
    private String dokumentId;

    @Column(nullable = false)
    private int wersja;

    @Column(name = "sciezka_pliku", nullable = false)
    private String sciezkaPliku;

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

    public String getDokumentId() {
        return dokumentId;
    }

    public void setDokumentId(String dokumentId) {
        this.dokumentId = dokumentId;
    }

    public int getWersja() {
        return wersja;
    }

    public void setWersja(int wersja) {
        this.wersja = wersja;
    }

    public String getSciezkaPliku() {
        return sciezkaPliku;
    }

    public void setSciezkaPliku(String sciezkaPliku) {
        this.sciezkaPliku = sciezkaPliku;
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
