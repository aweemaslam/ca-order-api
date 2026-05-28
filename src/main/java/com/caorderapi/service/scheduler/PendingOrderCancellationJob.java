package com.caorderapi.service.scheduler;

import com.caorderapi.config.ApplicationStatusConfigurations;
import com.caorderapi.repository.OrderRepository;
import com.caorderapi.service.IOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PendingOrderCancellationJob {

    @Value("${app.orders.reservation-ttl-minutes:15}")
    private Integer pendingOrderCancellationMs;
    private static final Integer PAGE_SIZE = 100;
    private final ApplicationStatusConfigurations applicationStatusConfigurations;
    private final IOrderService orderService;
    private final OrderRepository orderRepository;

    @Scheduled(fixedDelayString = "${app.jobs.pending-order-cancellation-ms:5000}")
    @SchedulerLock(name = "pendingOrderCancellationJob", lockAtLeastFor = "PT2S", lockAtMostFor = "PT20S")
    @Transactional
    public void publish() {
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);
        while (true) {
            Instant cutoff = Instant.now().minus(pendingOrderCancellationMs, ChronoUnit.MINUTES);
            Page<UUID> pendingOrders = orderRepository.findPendingOrdersOlderThan(applicationStatusConfigurations.getOrders().getInitialStatus(), cutoff, pageable);
            if (pendingOrders.isEmpty()) {
                break;
            }
            pendingOrders.getContent().forEach(oid -> {
                orderService.transitionOrderStatus(oid, applicationStatusConfigurations.getOrders().getCancelledStatus());
                log.info("Pending orderId %s cancelled".formatted(oid));
            });

            if (!pendingOrders.hasNext()) {
                break;
            }
            pageable = pendingOrders.nextPageable();
        }
    }
}

