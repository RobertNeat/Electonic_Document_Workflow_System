package robert_neat.document_api.dto.administracja;

import com.fasterxml.jackson.annotation.JsonValue;

/** Wartości 1:1 z TS union TypOperacjiAudytu (administracja.model.ts) — polskie znaki w wartości JSON. */
public enum TypOperacjiAudytu {
    ZMIANA_STANU_SPRAWY("Zmiana stanu sprawy"),
    PODPISANIE_DOKUMENTU("Podpisanie dokumentu"),
    REJESTRACJA_WPLATY("Rejestracja wpłaty"),
    REJESTRACJA_SPRAWY("Rejestracja sprawy"),
    WYSYLKA_PISMA("Wysyłka pisma"),
    DEKRETACJA("Dekretacja"),
    ZMIANA_DANYCH_UZYTKOWNIKA("Zmiana danych użytkownika");

    private final String wartosc;

    TypOperacjiAudytu(String wartosc) {
        this.wartosc = wartosc;
    }

    @JsonValue
    public String getWartosc() {
        return wartosc;
    }
}
