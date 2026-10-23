package robert_neat.document_api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import robert_neat.document_api.config.PrzesylkiStatusRabbitMqConfig;
import robert_neat.document_api.dto.korespondencja.StatusPrzesylki;
import robert_neat.document_api.dto.korespondencja.ZmianaStatusuPrzesylkiMessage;
import robert_neat.document_api.mapper.KorespondencjaMapper;

/** Konsument RabbitMQ zdarzeń zmiany statusu przesyłki, publikowanych przez delivery_mock.
 * Odnalezienie przesyłki po numerReferencyjny i jej modyfikacja odbywają się w jednej transakcji
 * wewnątrz KorespondencjaService.aktualizujStatus - encja wczytana tutaj (poza transakcją, na
 * wątku listenera) byłaby detached, gdyby przekazać ją dalej do modyfikacji. Każdy błąd (nieznany
 * numerReferencyjny, nierozpoznana wartość statusu) jest łapany i kończy przetwarzanie wiadomości
 * bez ponownego rzucenia - inaczej domyślny requeue-on-exception zapętliłby "poison" wiadomość
 * w nieskończoność (ten sam wzorzec co AkcjaZbiorowaConsumer). */
@Component
public class PrzesylkaStatusuConsumer {

    private static final Logger log = LoggerFactory.getLogger(PrzesylkaStatusuConsumer.class);

    private final KorespondencjaService korespondencjaService;

    public PrzesylkaStatusuConsumer(KorespondencjaService korespondencjaService) {
        this.korespondencjaService = korespondencjaService;
    }

    @RabbitListener(queues = PrzesylkiStatusRabbitMqConfig.QUEUE)
    public void obsluz(ZmianaStatusuPrzesylkiMessage message) {
        try {
            przetworz(message);
        } catch (RuntimeException ex) {
            log.error("Przetwarzanie zdarzenia zmiany statusu przesyłki numerReferencyjny={} nie powiodło się - wiadomość odrzucona bez ponownej kolejki",
                    message.numerReferencyjny(), ex);
        }
    }

    private void przetworz(ZmianaStatusuPrzesylkiMessage message) {
        StatusPrzesylki nowyStatus;
        try {
            nowyStatus = KorespondencjaMapper.statusFromDb(message.nowyStatus());
        } catch (IllegalArgumentException ex) {
            log.warn("Nierozpoznana wartość statusu '{}' dla przesyłki numerReferencyjny={} - wiadomość ignorowana",
                    message.nowyStatus(), message.numerReferencyjny());
            return;
        }

        boolean znaleziono = korespondencjaService.aktualizujStatus(message.numerReferencyjny(), nowyStatus).isPresent();
        if (!znaleziono) {
            log.warn("Przesyłka o numerze referencyjnym={} nie istnieje w document_api - wiadomość ignorowana", message.numerReferencyjny());
        }
    }
}
