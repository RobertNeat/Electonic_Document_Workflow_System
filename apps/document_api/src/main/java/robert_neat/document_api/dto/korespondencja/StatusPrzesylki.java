package robert_neat.document_api.dto.korespondencja;

import com.fasterxml.jackson.annotation.JsonValue;

public enum StatusPrzesylki {
    NADANE("nadane"),
    DORECZONE("doreczone"),
    ODEBRANE("odebrane"),
    ZWROT("zwrot"),
    AWIZO("awizo");

    private final String wartosc;

    StatusPrzesylki(String wartosc) {
        this.wartosc = wartosc;
    }

    @JsonValue
    public String getWartosc() {
        return wartosc;
    }
}
