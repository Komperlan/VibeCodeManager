package com.aiq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AiqApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiqApplication.class, args);
    }
}
