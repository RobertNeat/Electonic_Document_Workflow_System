package robert_neat.delivery_mock.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** Mapowanie tabeli `przesylka_dostawy` - stan przesyłki po stronie "dostawcy" (e-Doręczenia /
 * operator pocztowy). `numerReferencyjny` jest polem łączącym z `core.przesylka.numer_referencyjny`
 * w document_api (bez FK, referencja międzyschemowa). `kanal`/`status` przechowywane jako String
 * (CHECK constraint w bazie), bez @Enumerated - zgodnie z konwencją document_api/dms_service. */
@Entity
@Table(name = "przesylka_dostawy")
public class PrzesylkaDostawyEntity {

    @Id
    private String id;

    @Column(name = "numer_referencyjny", nullable = false, unique = true)
    private String numerReferencyjny;

    @Column(nullable = false)
    private String kanal;

    @Column(name = "adres_ade")
    private String adresAde;

    @Column(nullable = false)
    private String status;

    @Column(name = "data_nadania", nullable = false)
    private OffsetDateTime dataNadania;

    @Column(name = "data_statusu", nullable = false)
    private OffsetDateTime dataStatusu;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNumerReferencyjny() {
        return numerReferencyjny;
    }

    public void setNumerReferencyjny(String numerReferencyjny) {
        this.numerReferencyjny = numerReferencyjny;
    }

    public String getKanal() {
        return kanal;
    }

    public void setKanal(String kanal) {
        this.kanal = kanal;
    }

    public String getAdresAde() {
        return adresAde;
    }

    public void setAdresAde(String adresAde) {
        this.adresAde = adresAde;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
}
