package com.caorderapi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "app")
public class ApplicationStatusConfigurations {

    private StatusConfig orders;
    private StatusConfig orderItems;

    @Setter
    @Getter
    public static class StatusConfig {
        private String initialStatus;
        private String payTargetStatus;
        private String fulfillTargetStatus;
        private String cancelledStatus;
        private Integer reservationTtlMinutes;

    }
}
