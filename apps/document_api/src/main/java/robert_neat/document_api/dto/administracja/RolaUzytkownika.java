package robert_neat.document_api.dto.administracja;

import com.fasterxml.jackson.annotation.JsonValue;

/** Wire-format małymi literami - zgodnie z RolaUzytkownika w packages/document-shared-dto i CHECK ck_uzytkownik_rola. */
public enum RolaUzytkownika {
    ADMINISTRATOR("administrator"),
    KANCELARIA("kancelaria"),
    REFERENT("referent"),
    KIEROWNIK("kierownik"),
    KSIEGOWOSC("ksiegowosc");

    private final String wartosc;

    RolaUzytkownika(String wartosc) {
        this.wartosc = wartosc;
    }

    @JsonValue
    public String getWartosc() {
        return wartosc;
    }
}
