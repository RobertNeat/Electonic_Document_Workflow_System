package robert_neat.delivery_mock.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Topologia RabbitMQ dla zdarzeń zmiany statusu przesyłki (delivery_mock -> document_api).
 * Nazwana wymiana typu direct + explicit binding (nie domyślna exchange "") - ten sam wzorzec co
 * document_api/RabbitMqConfig (akcje zbiorowe), ale ODRĘBNA topologia (inny temat). Kolejka i
 * wymiana muszą być deklarowane identycznie po obu stronach (producent i konsument) - inaczej
 * zdarzenia opublikowane przed startem document_api byłyby nierutowalne i tracone.
 * JacksonJsonMessageConverter (nie Jackson2JsonMessageConverter) z tego samego powodu co w
 * document_api - ten moduł też jest na Jackson 3 (Spring Boot 4.1.1). */
@Configuration
public class RabbitMqConfig {

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

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
