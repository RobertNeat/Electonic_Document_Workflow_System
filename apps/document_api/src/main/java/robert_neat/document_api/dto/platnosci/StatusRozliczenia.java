package robert_neat.document_api.dto.platnosci;

import com.fasterxml.jackson.annotation.JsonValue;

public enum StatusRozliczenia {
    NIEROZLICZONA("nierozliczona"),
    CZESCIOWO_ROZLICZONA("czesciowo_rozliczona"),
    ROZLICZONA("rozliczona"),
    PRZETERMINOWANA("przeterminowana");

    private final String wartosc;

    StatusRozliczenia(String wartosc) {
        this.wartosc = wartosc;
    }

    @JsonValue
    public String getWartosc() {
        return wartosc;
    }
}
