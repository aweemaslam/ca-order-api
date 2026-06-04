package com.caorderapi.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

@Configuration
@Profile("test")
public class RedisTestContainerConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    // Static block guarantees Redis starts BEFORE Spring begins initialization
    static {
        redis.start();
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        // Programmatically injects the dynamic ports into Spring's environment properties
        MapPropertySource redisProperties = new MapPropertySource("redisTestProperties", Map.of(
                "spring.data.redis.host", redis.getHost(),
                "spring.data.redis.port", String.valueOf(redis.getMappedPort(6379))
        ));

        applicationContext.getEnvironment().getPropertySources().addFirst(redisProperties);
    }
}