package robert_neat.document_api.dto.korespondencja;

/** Payload odbierany z RabbitMQ (delivery_mock -> document_api) przy zmianie statusu przesyłki.
 * numerReferencyjny - pole łączące obie strony, nowyStatus jako String (nie enum) - konsument sam
 * mapuje i łapie nierozpoznaną wartość, zamiast wywalić się już w konwerterze JSON. Brak znacznika
 * czasu - document_api stempluje własny czas odbioru zdarzenia (OffsetDateTime.now()). */
public record ZmianaStatusuPrzesylkiMessage(
        String numerReferencyjny,
        String nowyStatus
) {
}
