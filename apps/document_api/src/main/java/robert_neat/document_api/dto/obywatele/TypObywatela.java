package robert_neat.document_api.dto.obywatele;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TypObywatela {
    OSOBA_FIZYCZNA("osoba_fizyczna"),
    PODMIOT("podmiot");

    private final String wartosc;

    TypObywatela(String wartosc) {
        this.wartosc = wartosc;
    }

    @JsonValue
    public String getWartosc() {
        return wartosc;
    }
}
