package com.caorderapi.service.scheduler;

import com.caorderapi.enums.AggregateType;
import com.caorderapi.enums.OutboxEventType;
import com.caorderapi.kafka.model.OrderEvent;
import com.caorderapi.kafka.producer.OrderEventProducer;
import com.caorderapi.model.OutboxEventEntity;
import com.caorderapi.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherJobTest {

    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private OrderEventProducer producer;
    @InjectMocks private OutboxPublisherJob job;

    private OutboxEventEntity buildEvent() {
        OutboxEventEntity e = new OutboxEventEntity();
        e.setId(UUID.randomUUID());
        e.setAggregateId(UUID.randomUUID().toString());
        e.setAggregateType(AggregateType.ORDER);
        e.setEventType(OutboxEventType.ORDER_CREATED);
        e.setProcessed(false);
        e.setRetryCount(0);
        com.caorderapi.dto.OutboxEntityPayload payload =
                new com.caorderapi.dto.OutboxEntityPayload("PENDING", Collections.emptyMap());
        e.setPayload(payload);
        return e;
    }

    @Test
    void publish_noPendingEvents_doesNothing() {
        when(outboxEventRepository.findTop200ByProcessedFalseOrderByCreatedAtAsc())
                .thenReturn(Collections.emptyList());
        job.publish();
        verify(producer, never()).publish(any());
    }

    @Test
    void publish_successfulEvent_marksProcessedTrue() {
        OutboxEventEntity event = buildEvent();
        when(outboxEventRepository.findTop200ByProcessedFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(event));
        doNothing().when(producer).publish(any());

        job.publish();

        assertThat(event.isProcessed()).isTrue();
        assertThat(event.getLastError()).isNull();
    }

    @Test
    void publish_producerThrows_incrementsRetryCountAndRecordsError() {
        OutboxEventEntity event = buildEvent();
        when(outboxEventRepository.findTop200ByProcessedFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(event));
        doThrow(new RuntimeException("Kafka down")).when(producer).publish(any());

        job.publish();

        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getLastError()).contains("Kafka down");
        assertThat(event.isProcessed()).isFalse();
    }

    @Test
    void publish_eventReachesMaxRetries_markedProcessedTrue() {
        OutboxEventEntity event = buildEvent();
        event.setRetryCount(4); // will become 5 = MAX
        when(outboxEventRepository.findTop200ByProcessedFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(event));
        doThrow(new RuntimeException("persistent failure")).when(producer).publish(any());

        job.publish();

        assertThat(event.getRetryCount()).isEqualTo(5);
        assertThat(event.isProcessed()).isTrue();
    }

    @Test
    void publish_buildsOrderEventCorrectly() {
        OutboxEventEntity event = buildEvent();
        when(outboxEventRepository.findTop200ByProcessedFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(event));
        doNothing().when(producer).publish(any());

        job.publish();

        ArgumentCaptor<OrderEvent> cap = ArgumentCaptor.forClass(OrderEvent.class);
        verify(producer).publish(cap.capture());
        OrderEvent sent = cap.getValue();
        assertThat(sent.aggregateId()).isEqualTo(event.getAggregateId());
        assertThat(sent.aggregateType()).isEqualTo("ORDER");
        assertThat(sent.eventType()).isEqualTo("ORDER_CREATED");
        assertThat(sent.payload()).isEqualTo(event.getPayload());
    }
}
