package robert_neat.document_api.dto.sprawy;

import com.fasterxml.jackson.annotation.JsonValue;

public enum KategoriaArchiwalna {
    A("A"),
    B5("B5"),
    B10("B10"),
    BE5("BE5"),
    BE10("BE10");

    private final String wartosc;

    KategoriaArchiwalna(String wartosc) {
        this.wartosc = wartosc;
    }

    @JsonValue
    public String getWartosc() {
        return wartosc;
    }
}
