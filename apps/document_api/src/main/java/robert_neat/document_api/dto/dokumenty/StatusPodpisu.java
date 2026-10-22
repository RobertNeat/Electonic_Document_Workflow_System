package robert_neat.document_api.dto.dokumenty;

import com.fasterxml.jackson.annotation.JsonValue;

public enum StatusPodpisu {
    BRAK("brak"),
    POPRAWNY("poprawny"),
    NIEPOPRAWNY("niepoprawny"),
    CERTYFIKAT_WYGASLY("certyfikat_wygasly"),
    CERTYFIKAT_UNIEWAZNIONY("certyfikat_uniewazniony");

    private final String wartosc;

    StatusPodpisu(String wartosc) {
        this.wartosc = wartosc;
    }

    @JsonValue
    public String getWartosc() {
        return wartosc;
    }
}
