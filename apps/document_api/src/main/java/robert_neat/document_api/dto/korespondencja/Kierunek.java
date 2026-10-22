package robert_neat.document_api.dto.korespondencja;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Kierunek {
    WPLYWAJACA("wplywajaca"),
    WYCHODZACA("wychodzaca");

    private final String wartosc;

    Kierunek(String wartosc) {
        this.wartosc = wartosc;
    }

    @JsonValue
    public String getWartosc() {
        return wartosc;
    }
}
