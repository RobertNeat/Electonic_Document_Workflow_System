package robert_neat.document_api.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Topologia RabbitMQ dla akcji zbiorowych: pojedyncza trwała kolejka + nazwana wymiana typu direct
 * z explicit bindingiem (zamiast domyślnej exchange ""), żeby zachować standardowy, rozszerzalny
 * wzorzec producer->exchange->queue (łatwe dodanie kolejnych routing keys/kolejek w przyszłości),
 * mimo że przy jednej kolejce wystarczyłoby publikowanie bezpośrednio na nią. JacksonJsonMessageConverter
 * (nie Jackson2JsonMessageConverter!) - ten moduł używa Jackson 3 (tools.jackson, Spring Boot 4.1.1
 * domyślnie), a na classpath nie ma com.fasterxml.jackson.databind (tylko jackson-annotations dla
 * @JsonValue) - Jackson2JsonMessageConverter by się nie uruchomił. */
@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE = "akcje-zbiorowe.exchange";
    public static final String QUEUE = "akcje-zbiorowe.queue";
    public static final String ROUTING_KEY = "akcje-zbiorowe";

    @Bean
    public Queue akcjeZbioroweQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public DirectExchange akcjeZbioroweExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Binding akcjeZbioroweBinding(Queue akcjeZbioroweQueue, DirectExchange akcjeZbioroweExchange) {
        return BindingBuilder.bind(akcjeZbioroweQueue).to(akcjeZbioroweExchange).with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
