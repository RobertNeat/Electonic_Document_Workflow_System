package robert_neat.document_api.dto.wymiana;

import com.fasterxml.jackson.annotation.JsonValue;

public enum StatusWymiany {
    SUKCES("sukces"),
    BLAD_WALIDACJI("blad_walidacji");

    private final String wartosc;

    StatusWymiany(String wartosc) {
        this.wartosc = wartosc;
    }

    @JsonValue
    public String getWartosc() {
        return wartosc;
    }
}
