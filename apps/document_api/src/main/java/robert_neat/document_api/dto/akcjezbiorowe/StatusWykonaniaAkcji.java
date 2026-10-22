package robert_neat.document_api.dto.akcjezbiorowe;

import com.fasterxml.jackson.annotation.JsonValue;

public enum StatusWykonaniaAkcji {
    TRWAJACA("trwajaca"),
    ZAKONCZONA("zakonczona");

    private final String wartosc;

    StatusWykonaniaAkcji(String wartosc) {
        this.wartosc = wartosc;
    }

    @JsonValue
    public String getWartosc() {
        return wartosc;
    }
}
