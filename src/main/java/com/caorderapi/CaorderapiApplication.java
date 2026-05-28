package com.caorderapi;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableFeignClients
@EnableSchedulerLock(defaultLockAtMostFor = "PT30S")
@EnableKafka
public class CaorderapiApplication {

	public static void main(String[] args) {
		SpringApplication.run(CaorderapiApplication.class, args);
	}

}
