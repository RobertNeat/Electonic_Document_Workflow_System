package robert_neat.document_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Mapowanie tabeli `akcja_zbiorowa_element`. Jeden wiersz per idCelu z UruchomAkcjeZbiorowaRequest,
 * tworzony z góry (status=null) przy uruchomieniu akcji - tabela `akcja_zbiorowa` nie ma kolumny
 * na listę celów, więc to ten wiersz jest zarówno rejestrem celów, jak i miejscem śledzenia
 * postępu per-element (status/komunikat aktualizowane przez konsumenta w miarę przetwarzania). */
@Entity
@Table(name = "akcja_zbiorowa_element")
public class AkcjaZbiorowaElementEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "akcja_zbiorowa_id", nullable = false)
    private AkcjaZbiorowaEntity akcjaZbiorowa;

    @Column(name = "id_celu", nullable = false)
    private String idCelu;

    /** null = jeszcze nieprzetworzony; wartość CHECK: 'sukces'/'blad' (zob. StatusElementuAkcji.getWartosc()). */
    @Column
    private String status;

    @Column(length = 1024)
    private String komunikat;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public AkcjaZbiorowaEntity getAkcjaZbiorowa() {
        return akcjaZbiorowa;
    }

    public void setAkcjaZbiorowa(AkcjaZbiorowaEntity akcjaZbiorowa) {
        this.akcjaZbiorowa = akcjaZbiorowa;
    }

    public String getIdCelu() {
        return idCelu;
    }

    public void setIdCelu(String idCelu) {
        this.idCelu = idCelu;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getKomunikat() {
        return komunikat;
    }

    public void setKomunikat(String komunikat) {
        this.komunikat = komunikat;
    }
}
