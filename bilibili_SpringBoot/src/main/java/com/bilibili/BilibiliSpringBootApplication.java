package com.bilibili;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BilibiliSpringBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(BilibiliSpringBootApplication.class, args);
    }
}
