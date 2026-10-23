package robert_neat.delivery_mock.dto;

/** Payload publikowany na RabbitMQ przy zmianie statusu przesyłki. numerReferencyjny - pole
 * łączące obie strony (document_api odnajduje swoją Przesylke po nim). nowyStatus jako String
 * (nie enum) - wspólny słownik wartości
 * delivery_mock/document_api jest identyczny (zob. PrzesylkaDostawyService.STATUSY), ale String
 * pozwala konsumentowi samemu złapać i odrzucić nierozpoznaną wartość, zamiast wywalić się już w
 * konwerterze JSON przed jego własną obsługą błędów. document_api samo stempluje datę odbioru
 * zdarzenia (OffsetDateTime.now()), więc nie wysyłamy tu żadnego znacznika czasu. */
public record ZmianaStatusuPrzesylkiMessage(
        String numerReferencyjny,
        String nowyStatus
) {
}
