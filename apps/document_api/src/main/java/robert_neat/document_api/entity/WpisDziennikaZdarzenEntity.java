package robert_neat.document_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

/** Mapowanie tabeli `wpis_dziennika_zdarzen`. Pole `typOperacji` przechowywane jako string DB-value
 * (pełne polskie frazy, 1:1 z TypOperacjiAudytu#getWartosc(), np. "Zmiana stanu sprawy") - brak CHECK
 * constraint na tej kolumnie w 009-administracja.xml, ale wartości seeda i enuma muszą się zgadzać,
 * więc traktujemy je jak CHECK-owaną kolumnę mimo braku jawnej bazowej walidacji. */
@Entity
@Table(name = "wpis_dziennika_zdarzen")
public class WpisDziennikaZdarzenEntity {

    @Id
    private String id;

    @Column(name = "data_czas", nullable = false)
    private OffsetDateTime dataCzas;

    @Column(nullable = false)
    private String uzytkownik;

    @Column(name = "typ_operacji", nullable = false)
    private String typOperacji;

    @Column(nullable = false)
    private String obiekt;

    private String szczegoly;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public OffsetDateTime getDataCzas() {
        return dataCzas;
    }

    public void setDataCzas(OffsetDateTime dataCzas) {
        this.dataCzas = dataCzas;
    }

    public String getUzytkownik() {
        return uzytkownik;
    }

    public void setUzytkownik(String uzytkownik) {
        this.uzytkownik = uzytkownik;
    }

    public String getTypOperacji() {
        return typOperacji;
    }

    public void setTypOperacji(String typOperacji) {
        this.typOperacji = typOperacji;
    }

    public String getObiekt() {
        return obiekt;
    }

    public void setObiekt(String obiekt) {
        this.obiekt = obiekt;
    }

    public String getSzczegoly() {
        return szczegoly;
    }

    public void setSzczegoly(String szczegoly) {
        this.szczegoly = szczegoly;
    }
}
