package robert_neat.document_api.dto.korespondencja;

import com.fasterxml.jackson.annotation.JsonValue;

public enum KanalWysylki {
    E_DORECZENIA("e-doreczenia"),
    OPERATOR_POCZTOWY("operator_pocztowy"),
    EMAIL("email");

    private final String wartosc;

    KanalWysylki(String wartosc) {
        this.wartosc = wartosc;
    }

    @JsonValue
    public String getWartosc() {
        return wartosc;
    }
}
