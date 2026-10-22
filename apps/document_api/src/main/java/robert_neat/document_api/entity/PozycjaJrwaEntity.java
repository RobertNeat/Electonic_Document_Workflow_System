package robert_neat.document_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Mapowanie tabeli `pozycja_jrwa`. PK to `symbol` (nie generowany UUID `id`, jak w pozostałych
 * encjach) - JRWA jest słownikiem kluczowanym naturalnym symbolem klasyfikacyjnym. */
@Entity
@Table(name = "pozycja_jrwa")
public class PozycjaJrwaEntity {

    @Id
    private String symbol;

    @Column(nullable = false)
    private String haslo;

    /** Wartość zgodna z CHECK constraint (ck_pozycja_jrwa_kategoria_archiwalna): A, B5, B10, BE5, BE10. */
    @Column(name = "kategoria_archiwalna", nullable = false)
    private String kategoriaArchiwalna;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getHaslo() {
        return haslo;
    }

    public void setHaslo(String haslo) {
        this.haslo = haslo;
    }

    public String getKategoriaArchiwalna() {
        return kategoriaArchiwalna;
    }

    public void setKategoriaArchiwalna(String kategoriaArchiwalna) {
        this.kategoriaArchiwalna = kategoriaArchiwalna;
    }
}
