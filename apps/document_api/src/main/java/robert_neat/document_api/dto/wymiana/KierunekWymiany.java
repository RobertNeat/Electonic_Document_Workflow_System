package robert_neat.document_api.dto.wymiana;

import com.fasterxml.jackson.annotation.JsonValue;

public enum KierunekWymiany {
    IMPORT("import"),
    EKSPORT("eksport");

    private final String wartosc;

    KierunekWymiany(String wartosc) {
        this.wartosc = wartosc;
    }

    @JsonValue
    public String getWartosc() {
        return wartosc;
    }
}
