package robert_neat.delivery_mock.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import robert_neat.delivery_mock.config.RabbitMqConfig;
import robert_neat.delivery_mock.dto.ZmianaStatusuPrzesylkiMessage;

/** Publikacja zdarzenia zmiany statusu przesyłki do document_api przez RabbitMQ. Wołane z
 * SymulacjaDostawyService PO zatwierdzeniu lokalnego zapisu - awaria/niedostępność brokera nie
 * może wycofać lokalnej zmiany statusu. */
@Component
public class ZmianaStatusuPrzesylkiPublisher {

    private final RabbitTemplate rabbitTemplate;

    public ZmianaStatusuPrzesylkiPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /** Rzuca AmqpException przy błędzie publikacji (broker niedostępny itp.) - wywołujący decyduje,
     * czy to fatalne (zob. SymulacjaDostawyService, które degraduje błąd do ostrzeżenia). */
    public void publikuj(String numerReferencyjny, String nowyStatus) {
        rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE, RabbitMqConfig.ROUTING_KEY,
                new ZmianaStatusuPrzesylkiMessage(numerReferencyjny, nowyStatus));
    }
}
