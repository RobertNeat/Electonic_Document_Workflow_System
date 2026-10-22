package robert_neat.document_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** Mapowanie tabeli `przesylka`. Pola `kanal`/`status`/`kierunek` przechowywane jako string
 * DB-value (CHECK constraints) - analogicznie do WersjaDokumentuEntity.statusPodpisu. */
@Entity
@Table(name = "przesylka")
public class PrzesylkaEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprawa_id", nullable = false)
    private SprawaEntity sprawa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dokument_id", nullable = false)
    private DokumentEntity dokument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "obywatel_id", nullable = false)
    private ObywatelEntity obywatel;

    @Column(nullable = false)
    private String kanal;

    @Column(nullable = false)
    private String status;

    @Column(name = "numer_referencyjny", nullable = false, unique = true)
    private String numerReferencyjny;

    @Column(name = "data_nadania", nullable = false)
    private OffsetDateTime dataNadania;

    @Column(name = "data_statusu", nullable = false)
    private OffsetDateTime dataStatusu;

    @Column(nullable = false)
    private String kierunek;

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

    public DokumentEntity getDokument() {
        return dokument;
    }

    public void setDokument(DokumentEntity dokument) {
        this.dokument = dokument;
    }

    public ObywatelEntity getObywatel() {
        return obywatel;
    }

    public void setObywatel(ObywatelEntity obywatel) {
        this.obywatel = obywatel;
    }

    public String getKanal() {
        return kanal;
    }

    public void setKanal(String kanal) {
        this.kanal = kanal;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNumerReferencyjny() {
        return numerReferencyjny;
    }

    public void setNumerReferencyjny(String numerReferencyjny) {
        this.numerReferencyjny = numerReferencyjny;
    }

    public OffsetDateTime getDataNadania() {
        return dataNadania;
    }

    public void setDataNadania(OffsetDateTime dataNadania) {
        this.dataNadania = dataNadania;
    }

    public OffsetDateTime getDataStatusu() {
        return dataStatusu;
    }

    public void setDataStatusu(OffsetDateTime dataStatusu) {
        this.dataStatusu = dataStatusu;
    }

    public String getKierunek() {
        return kierunek;
    }

    public void setKierunek(String kierunek) {
        this.kierunek = kierunek;
    }
}
