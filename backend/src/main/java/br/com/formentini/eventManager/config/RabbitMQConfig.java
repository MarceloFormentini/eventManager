package br.com.formentini.eventManager.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;

import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RabbitMQConfig {

    public static final String EVENT_EXCHANGE = "event.exchange";
    public static final String EVENT_CREATED_QUEUE = "event.created.queue";
    public static final String EVENT_CREATED_ROUTING_KEY = "event.created.v1";

    @Bean
    public TopicExchange eventExchange() {
        return new TopicExchange(EVENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue eventCreatedQueue() {
        return new Queue(EVENT_CREATED_QUEUE, true);
    }

    @Bean
    public Binding eventCreatedBinding(
            Queue eventCreatedQueue,
            TopicExchange eventExchange
    ) {
        return BindingBuilder
                .bind(eventCreatedQueue)
                .to(eventExchange)
                .with(EVENT_CREATED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jacksonJsonMessageConverter(
            JsonMapper jsonMapper
    ) {
        return new JacksonJsonMessageConverter(
                jsonMapper,
                "br.com.formentini.eventManager.event.messaging"
        );
    }
}
