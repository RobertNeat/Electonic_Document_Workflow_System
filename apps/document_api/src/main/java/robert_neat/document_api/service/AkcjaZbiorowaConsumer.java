package robert_neat.document_api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import robert_neat.document_api.config.RabbitMqConfig;
import robert_neat.document_api.dto.akcjezbiorowe.AkcjaZbiorowaMessage;
import robert_neat.document_api.dto.akcjezbiorowe.TypAkcjiZbiorowej;
import robert_neat.document_api.entity.AkcjaZbiorowaElementEntity;
import robert_neat.document_api.entity.AkcjaZbiorowaEntity;
import robert_neat.document_api.mapper.AkcjeZbioroweMapper;
import robert_neat.document_api.repository.AkcjaZbiorowaElementRepository;
import robert_neat.document_api.repository.AkcjaZbiorowaRepository;

import java.util.List;

/** Konsument RabbitMQ dla akcji zbiorowych. Wiadomość jest tylko "claim check" (akcjaId) - pełny
 * stan (typ akcji, parametry, lista elementów) jest odczytywany z bazy, będącej źródłem prawdy.
 * Metoda @RabbitListener NIE jest otoczona jedną transakcją: każdy element commituje się osobno
 * przez AkcjaZbiorowaProcessingService.przetworzElement (REQUIRES_NEW) - dzięki temu GET .../status
 * widzi narastający postęp w trakcie przetwarzania, a nie tylko po zakończeniu całej partii.
 * Każdy błąd (w tym nieznana akcja, zepsuty JSON parametrów) jest łapany i kończy przetwarzanie
 * wiadomości bez ponownego rzucenia - inaczej domyślny requeue-on-exception zapętliłby "poison"
 * wiadomość w nieskończoność. */
@Component
public class AkcjaZbiorowaConsumer {

    private static final Logger log = LoggerFactory.getLogger(AkcjaZbiorowaConsumer.class);

    private final AkcjaZbiorowaRepository akcjaZbiorowaRepository;
    private final AkcjaZbiorowaElementRepository akcjaZbiorowaElementRepository;
    private final AkcjaZbiorowaProcessingService processingService;

    public AkcjaZbiorowaConsumer(AkcjaZbiorowaRepository akcjaZbiorowaRepository,
                                  AkcjaZbiorowaElementRepository akcjaZbiorowaElementRepository,
                                  AkcjaZbiorowaProcessingService processingService) {
        this.akcjaZbiorowaRepository = akcjaZbiorowaRepository;
        this.akcjaZbiorowaElementRepository = akcjaZbiorowaElementRepository;
        this.processingService = processingService;
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE)
    public void obsluz(AkcjaZbiorowaMessage message) {
        try {
            przetworz(message);
        } catch (RuntimeException ex) {
            log.error("Przetwarzanie akcji zbiorowej id={} nie powiodło się - wiadomość odrzucona bez ponownej kolejki", message.akcjaId(), ex);
        }
    }

    private void przetworz(AkcjaZbiorowaMessage message) {
        AkcjaZbiorowaEntity akcja = akcjaZbiorowaRepository.findById(message.akcjaId()).orElse(null);
        if (akcja == null) {
            log.warn("Akcja zbiorowa id={} nie istnieje w bazie - wiadomość ignorowana", message.akcjaId());
            return;
        }

        TypAkcjiZbiorowej typAkcji = AkcjeZbioroweMapper.typAkcjiFromDb(akcja.getTypAkcji());
        String parametryJson = akcja.getParametry();
        List<AkcjaZbiorowaElementEntity> elementy = akcjaZbiorowaElementRepository.findByAkcjaZbiorowa_Id(akcja.getId());

        for (AkcjaZbiorowaElementEntity element : elementy) {
            processingService.przetworzElement(element.getId(), typAkcji, parametryJson, message.zleceniodawca());
        }

        processingService.zakonczAkcje(akcja.getId());
    }
}
