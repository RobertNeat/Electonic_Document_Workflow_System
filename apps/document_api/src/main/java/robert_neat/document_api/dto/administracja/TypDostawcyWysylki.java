package robert_neat.document_api.dto.administracja;

import com.fasterxml.jackson.annotation.JsonValue;

/** Normalizacja wg dto-spec.md: "email" (nie "e-mail" jak w dawnym mocku frontu administracji). */
public enum TypDostawcyWysylki {
    E_DORECZENIA("e-doreczenia"),
    OPERATOR_POCZTOWY("operator_pocztowy"),
    EMAIL("email");

    private final String wartosc;

    TypDostawcyWysylki(String wartosc) {
        this.wartosc = wartosc;
    }

    @JsonValue
    public String getWartosc() {
        return wartosc;
    }
}
