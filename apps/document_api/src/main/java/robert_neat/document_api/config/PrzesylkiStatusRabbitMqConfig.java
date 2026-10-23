package robert_neat.document_api.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Topologia RabbitMQ dla zdarzeń zmiany statusu przesyłki, publikowanych przez delivery_mock.
 * ODRĘBNA od topologii akcji zbiorowych (RabbitMqConfig) - inny producent, inny temat, zgodnie
 * z zasadą nie mieszania niezwiązanych topologii w jednej klasie konfiguracyjnej. Nazwy kolejki/wymiany/
 * routing key muszą być identyczne z deklaracją w delivery_mock (zob.
 * delivery_mock/config/RabbitMqConfig) - inaczej zdarzenia opublikowane zanim ten serwis
 * zadeklarował swoją kolejkę byłyby nierutowalne i tracone. Konwerter JSON
 * (JacksonJsonMessageConverter) jest już zarejestrowany jako jedyny bean MessageConverter w
 * RabbitMqConfig - nie deklarujemy tu drugiego (Boot wymaga unikalnego beana tego typu, inaczej
 * cichym fallbackiem wraca do SimpleMessageConverter, co zepsułoby też akcje zbiorowe). */
@Configuration
public class PrzesylkiStatusRabbitMqConfig {

    public static final String EXCHANGE = "przesylki-status.exchange";
    public static final String QUEUE = "przesylki-status.queue";
    public static final String ROUTING_KEY = "przesylki-status";

    @Bean
    public Queue przesylkiStatusQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public DirectExchange przesylkiStatusExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Binding przesylkiStatusBinding(Queue przesylkiStatusQueue, DirectExchange przesylkiStatusExchange) {
        return BindingBuilder.bind(przesylkiStatusQueue).to(przesylkiStatusExchange).with(ROUTING_KEY);
    }
}
