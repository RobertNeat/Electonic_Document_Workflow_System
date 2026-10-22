package robert_neat.document_api.dto.akcjezbiorowe;

import com.fasterxml.jackson.annotation.JsonValue;

public enum StatusElementuAkcji {
    SUKCES("sukces"),
    BLAD("blad");

    private final String wartosc;

    StatusElementuAkcji(String wartosc) {
        this.wartosc = wartosc;
    }

    @JsonValue
    public String getWartosc() {
        return wartosc;
    }
}
