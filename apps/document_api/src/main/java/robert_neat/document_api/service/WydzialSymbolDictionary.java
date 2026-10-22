package robert_neat.document_api.service;

import java.util.Map;

/**
 * Słownik symboli wydziałów do generowania znaku sprawy ({SYMBOL}.{JRWA}.{NUMER}.{ROK}).
 * Źródło: istniejące znaki spraw w seedzie demo (db/changelog/demo/100-seed.xml) -
 * "Wydział Ochrony Środowiska" -> WO.6220...; "Wydział Organizacyjny" -> OR.1510...;
 * "Wydział Księgowości" -> FK.3120.... "Wydział Komunikacji" (z tabeli `wydzial`, bez przykładu
 * znaku w seedzie spraw) otrzymuje symbol KM, przyjęty analogicznie do konwencji nazw wydziałów
 * urzędów (skrót pierwszych liter nazwy funkcjonalnej wydziału).
 */
final class WydzialSymbolDictionary {

    private static final Map<String, String> SYMBOLE = Map.of(
            "Wydział Ochrony Środowiska", "WO",
            "Wydział Organizacyjny", "OR",
            "Wydział Księgowości", "FK",
            "Wydział Komunikacji", "KM"
    );

    private WydzialSymbolDictionary() {
    }

    /** Zwraca symbol wydziału lub null, jeśli wydział nie jest w słowniku (do odrzucenia 400 przez wołającego). */
    static String symbolFor(String wydzial) {
        return SYMBOLE.get(wydzial);
    }
}
