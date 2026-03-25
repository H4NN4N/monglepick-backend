package com.monglepick.monglepickbackend;

import com.monglepick.monglepickbackend.domain.reward.config.QuotaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(QuotaProperties.class)
public class MonglepickBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonglepickBackendApplication.class, args);
    }

}
