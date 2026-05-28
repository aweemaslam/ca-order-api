package com.caorderapi.kafka.producer;

import com.caorderapi.kafka.model.OrderEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @Value("${spring.kafka.topic.order-event:order-events}")
    private String topic;

    public void publish(OrderEvent event) {
        kafkaTemplate.send(topic, event.aggregateId(), event);
    }
}