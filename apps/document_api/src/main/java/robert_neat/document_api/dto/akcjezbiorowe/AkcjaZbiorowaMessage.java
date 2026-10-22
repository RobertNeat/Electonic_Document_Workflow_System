package robert_neat.document_api.dto.akcjezbiorowe;

/** Wewnętrzny payload RabbitMQ (claim-check) - konsument odczytuje tylko akcjaId i ładuje pełny
 * stan z bazy (źródło prawdy), żeby uniknąć niespójności, gdyby wiadomość była przetwarzana z
 * opóźnieniem po ewentualnej dalszej zmianie encji. zleceniodawca to "osoba" rozwiązana z JWT
 * w kontrolerze (watek listenera nie ma SecurityContext) - używana jako autor wpisów metryki /
 * autor nowej wersji dokumentu, tam gdzie dany typ akcji tego wymaga. */
public record AkcjaZbiorowaMessage(
        String akcjaId,
        String zleceniodawca
) {
}
